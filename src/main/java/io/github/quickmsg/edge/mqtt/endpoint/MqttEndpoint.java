package io.github.quickmsg.edge.mqtt.endpoint;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.MqttContext;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.github.quickmsg.edge.mqtt.msg.WillMessage;
import io.github.quickmsg.edge.mqtt.pair.*;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.proxy.ProxyMessage;
import io.github.quickmsg.edge.mqtt.retry.RetryMessage;
import io.github.quickmsg.edge.mqtt.retry.RetryTask;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.github.quickmsg.edge.mqtt.util.MessageUtils;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author luxurong
 */

public class MqttEndpoint implements Endpoint<Packet> {


    private static final Pattern SHARE_MESSAGE_PATTERN = Pattern.compile("\\$share/(.*)");

    private final Connection connection;

    @Override
    public WillMessage getWillMessage() {
        return willMessage;
    }
    @Override
    public void setWillMessage(WillMessage willMessage) {
        this.willMessage = willMessage;
    }

    private WillMessage willMessage;
    private String clientId;

    private String clientIp;

    private MqttVersion version;

    private MqttProperties properties;

    private MqttProperties willProperties;

    private final long connectTime;



    private volatile boolean connected = false;

    private volatile boolean closed = true;

    private volatile boolean keepSession;


    private final InitConfig.MqttConfig mqttConfig;


    private final transient AtomicInteger atomicInteger = new AtomicInteger();


    private final Map<Integer, PublishPacket> qos2Cache = new ConcurrentHashMap<>();

    /**
     * 0  client close
     * 1  server close
     * 2  kicked
     * 3  beats-kill
     * 4  disconnect
     */
    private volatile int closeCode = 0;


    private volatile int receiveMaxMessageSize;

    private ProxyMessage proxyMessage;


    private final List<SubscribeTopic> subscribeTopics;

    private final MqttContext mqttContext;

    public String getClientIp() {
        return clientIp;
    }

    public MqttEndpoint(MqttContext mqttContext, InitConfig.MqttConfig mqttConfig, Connection connection) {
        this.mqttContext = mqttContext;
        this.mqttConfig = mqttConfig;
        this.connection = connection;
        this.clientIp = connection.channel().remoteAddress().toString().split(":")[0];
        this.connectTime = System.currentTimeMillis();
        this.subscribeTopics = new ArrayList<>();
        if (mqttConfig.proxy()) {
            AttributeKey<ProxyMessage> attributeKey = AttributeKey.valueOf("proxy");
            this.proxyMessage = connection.channel().attr(attributeKey).get();
            if (this.proxyMessage != null) {
                this.clientIp = this.proxyMessage.getSourceAddress();
            }
        }
        this.readIdle(this.mqttConfig.connectTimeout(), this::close);
    }

    public boolean isConnected() {
        return connected;
    }

    public int getCloseCode() {
        return closeCode;
    }

    public void setCloseCode(int closeCode) {
        this.closeCode = closeCode;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }


    public boolean isKeepSession() {
        return keepSession;
    }

    public void setKeepSession(boolean keepSession) {
        this.keepSession = keepSession;
    }

    public List<SubscribeTopic> getSubscribeTopics() {
        return subscribeTopics;
    }




    @Override
    public Flux<Packet> receive() {
        return connection
                .inbound()
                .receiveObject()
                .cast(MqttMessage.class)
                .map(this::transfer);
    }

    @Override
    public boolean isMqtt5() {
        return this.version != null && this.version == MqttVersion.MQTT_5;
    }

    @Override
    public boolean connected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public boolean isWriteable() {
        return connection.channel().isWritable();
    }

    @Override
    public MqttProperties connectProperties() {
        return this.properties;
    }

    @Override
    public MqttProperties willProperties() {
        return this.willProperties;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public InitConfig.MqttConfig getMqttConfig() {
        return this.mqttConfig;
    }

    @Override
    public String getClientId() {
        return this.clientId;
    }

    @Override
    public boolean isClosed() {
        return this.closed || !connection.channel().isActive();
    }

    @Override
    public void readIdle(long keeps, Runnable runnable) {
        connection.onReadIdle(keeps, runnable);
    }

    @Override
    public void writeIdle(long keeps, Runnable runnable) {
        connection.onWriteIdle(keeps, runnable);
    }

    @Override
    public void readWriteIdle(long keeps, Runnable runnable) {
        connection.onReadIdle(keeps, runnable);
        connection.onWriteIdle(keeps, runnable);
    }

    @Override
    public void close() {
        connection.dispose();
    }

    @Override
    public boolean cacheQos2Message(PublishPacket packet) {
        if (qos2Cache.containsKey(packet.messageId())) {
            return false;
        }
        qos2Cache.put(packet.messageId(), packet);
        return true;
    }


    @Override
    public PublishPacket removeQos2Message(int messageId) {
        return qos2Cache.remove(messageId);
    }

    @Override
    public PublishPacket getQos2Message(int messageId) {
        return qos2Cache.get(messageId);
    }

    @Override
    public ProxyMessage getProxyMessage() {
        return this.proxyMessage;
    }

    @Override
    public int generateMessageId() {
        int index = atomicInteger.incrementAndGet();
        if (index > 65535) {
            if (atomicInteger.compareAndSet(index, 1)) {
                return 1;
            } else {
                return atomicInteger.incrementAndGet();
            }
        }
        return index;
    }


    @Override
    public void onClose(Runnable close) {
        connection.onDispose(close::run);
    }

    private Packet transfer(MqttMessage mqttMessage) {
        return switch (mqttMessage.fixedHeader().messageType()) {
            case CONNECT -> {
                var connectMessage = (MqttConnectMessage) mqttMessage;
                ConnectPacket.ConnectUserDetail userDetail = null;
                if (connectMessage.variableHeader().hasUserName() || connectMessage.variableHeader().hasPassword()) {
                    userDetail = new ConnectPacket.ConnectUserDetail(connectMessage.payload().userName(),
                            connectMessage.payload().passwordInBytes());
                }
                this.version = MqttVersion.fromProtocolNameAndLevel(connectMessage.variableHeader().name(), (byte) connectMessage.variableHeader().version());
                WillMessage willMessage = null;
                if (connectMessage.variableHeader().isWillFlag()) {
                    willMessage = new WillMessage(connectMessage.variableHeader().isWillRetain(),
                            connectMessage.payload().willTopic(), connectMessage.variableHeader().willQos()
                            , connectMessage.payload().willMessageInBytes());
                }
                this.willProperties = connectMessage.payload().willProperties();
                this.properties = connectMessage.variableHeader().properties();
                this.setClientId(connectMessage.payload().clientIdentifier());
                var connectPair = this.isMqtt5() ? this.parserConnectPair(this.properties) : null;
                var willPair = this.isMqtt5() ? this.parserWillPair(this.willProperties) : null;
                this.readIdle(2000, this::delayClose);
                yield new ConnectPacket(this,
                        userDetail, willMessage,
                        connectMessage.variableHeader().isCleanSession(),
                        this.version,
                        connectMessage.variableHeader().keepAliveTimeSeconds(),
                        System.currentTimeMillis(),
                        connectPair,
                        this.proxyMessage,
                        willPair);
            }
            case PUBLISH -> {
                var publishMessage = (MqttPublishMessage) mqttMessage;
                var pair = this.isMqtt5() ? this.parserPublishPair(publishMessage) : null;
                var qos = publishMessage.fixedHeader().qosLevel().value();
                yield new PublishPacket(this, publishMessage.variableHeader().packetId(),
                        publishMessage.variableHeader().topicName(), qos,
                        MessageUtils.readByteBuf(publishMessage.payload()), publishMessage.fixedHeader().isRetain(),
                        false, qos > 0, System.currentTimeMillis(), pair);

            }

            case PUBACK -> {
                var ackPair = this.isMqtt5() ? this.parserAckPair(mqttMessage) : null;

                yield new PublishAckPacket(this,
                        ((MqttPubReplyMessageVariableHeader) mqttMessage.variableHeader()).messageId(),
                        ((MqttPubReplyMessageVariableHeader) mqttMessage.variableHeader()).reasonCode()
                        , System.currentTimeMillis(), ackPair);
            }
            case PUBREC -> {
                var ackPair = this.isMqtt5() ? this.parserAckPair(mqttMessage) : null;
                yield new PublishRecPacket(this,
                        ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(),
                        ((MqttPubReplyMessageVariableHeader) mqttMessage.variableHeader()).reasonCode()
                        , System.currentTimeMillis(), false, true, ackPair);
            }
            case PUBREL -> {
                var ackPair = this.isMqtt5() ? this.parserAckPair(mqttMessage) : null;
                yield new PublishRelPacket(this,
                        ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(),
                        ((MqttPubReplyMessageVariableHeader) mqttMessage.variableHeader()).reasonCode(),
                        System.currentTimeMillis(), false, true,ackPair);
            }

            case PUBCOMP -> {
                var ackPair = this.isMqtt5() ? this.parserAckPair(mqttMessage) : null;
                yield new PublishCompPacket(this,
                        ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(),
                        ((MqttPubReplyMessageVariableHeader) mqttMessage.variableHeader()).reasonCode(),
                        System.currentTimeMillis(), ackPair);
            }
            case SUBSCRIBE -> {
                var subscribeMessage = (MqttSubscribeMessage) mqttMessage;
                var subscribeInfos =
                        subscribeMessage
                                .payload()
                                .topicSubscriptions()
                                .stream()
                                .map(mqttTopicSubscription -> {
                                    var matcher = SHARE_MESSAGE_PATTERN.matcher(mqttTopicSubscription.topicFilter());
                                    SubscribeTopic subscribeTopic;
                                    if (matcher.find()) {
                                        subscribeTopic = new SubscribeTopic(clientId, matcher.group(1),
                                                mqttTopicSubscription.qualityOfService().value(), true);
                                    } else {
                                        subscribeTopic = new SubscribeTopic(clientId, mqttTopicSubscription.topicFilter(),
                                                mqttTopicSubscription.qualityOfService().value(), false);
                                    }
                                    return subscribeTopic;

                                })
                                .collect(Collectors.toSet());
                var subPair = this.isMqtt5() ? this.parserSubscribePair(subscribeMessage) : null;
                yield new SubscribePacket(this, subscribeMessage.variableHeader().messageId(), subscribeInfos,
                        System.currentTimeMillis(), subPair);
            }
            case UNSUBSCRIBE -> {
                var unsubscribeMessage = (MqttUnsubscribeMessage) mqttMessage;
                var unSubPair = this.isMqtt5() ? this.parserUnSubscribePair(unsubscribeMessage) : null;
                Map<String, Boolean> topics = new HashMap<>();
                for (String topicFilter : unsubscribeMessage.payload().topics()) {
                    var matcher = SHARE_MESSAGE_PATTERN.matcher(topicFilter);
                    if (matcher.find()) {
                        topics.put(matcher.group(1), true);
                    } else {
                        topics.put(topicFilter, false);
                    }
                }
                yield new UnsubscribePacket(this, unsubscribeMessage.idAndPropertiesVariableHeader().messageId(),
                        topics, System.currentTimeMillis(), unSubPair);

            }
            case DISCONNECT -> {
                var disconnectPair = this.isMqtt5() ? this.parserDisconnectPair(mqttMessage) : null;
                yield new DisconnectPacket(this, clientId, clientIp, System.currentTimeMillis(), disconnectPair);
            }
            case PINGREQ -> new PingPacket(this, clientId, clientIp, System.currentTimeMillis());
            case AUTH -> {
                var authPair = this.isMqtt5() ? this.parserAuthPair(mqttMessage) : null;
                yield new AuthPacket(this, clientId, clientIp, System.currentTimeMillis(), authPair);
            }
            default -> throw new IllegalStateException("Unexpected value: " + mqttMessage);
        };
    }


    private void delayClose() {
        connection.dispose();
    }

    @SuppressWarnings("unchecked")
    private WillPair parserWillPair(MqttProperties willProperties) {
        int willDelayInterval = getInt(willProperties, MqttProperties.MqttPropertyType.WILL_DELAY_INTERVAL);
        int payloadFormatIndicator = getInt(willProperties, MqttProperties.MqttPropertyType.PAYLOAD_FORMAT_INDICATOR);
        int messageExpiryInterval = getInt(willProperties, MqttProperties.MqttPropertyType.PUBLICATION_EXPIRY_INTERVAL);
        String contextType = getString(willProperties, MqttProperties.MqttPropertyType.CONTENT_TYPE);
        String responseTopic = getString(willProperties, MqttProperties.MqttPropertyType.RESPONSE_TOPIC);
        byte[] correlationData = getBytes(willProperties, MqttProperties.MqttPropertyType.CORRELATION_DATA);
        List<MqttProperties.StringProperty> userProperty = (List<MqttProperties.StringProperty>) willProperties.getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        return new WillPair(willDelayInterval, payloadFormatIndicator,
                messageExpiryInterval, contextType, responseTopic, correlationData, userProperty);
    }

    @SuppressWarnings("unchecked")
    private ConnectPair parserConnectPair(MqttProperties properties) {
        int sessionExpiry = getInt(properties, MqttProperties.MqttPropertyType.SESSION_EXPIRY_INTERVAL);
        int receiveMaximum = getInt(properties, MqttProperties.MqttPropertyType.RECEIVE_MAXIMUM);
        int maxPacketSize = getInt(properties, MqttProperties.MqttPropertyType.MAXIMUM_PACKET_SIZE);
        int maxTopicAliasMaximum = getInt(properties, MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM);
        int requestResponseInformation = getInt(properties, MqttProperties.MqttPropertyType.REQUEST_RESPONSE_INFORMATION);
        int requestProblemInformation = getInt(properties, MqttProperties.MqttPropertyType.REQUEST_PROBLEM_INFORMATION);
        List<MqttProperties.StringProperty> userProperty = (List<MqttProperties.StringProperty>) properties.getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        String authMethod = getString(properties, MqttProperties.MqttPropertyType.AUTHENTICATION_METHOD);
        byte[] authData = getBytes(properties, MqttProperties.MqttPropertyType.AUTHENTICATION_DATA);
        return new ConnectPair(sessionExpiry, receiveMaximum, maxPacketSize, maxTopicAliasMaximum
                , requestResponseInformation, requestProblemInformation, userProperty, authMethod, authData);
    }

    @SuppressWarnings("unchecked")
    private AuthPair parserAuthPair(MqttMessage mqttMessage) {
        var header = (MqttReasonCodeAndPropertiesVariableHeader) mqttMessage.variableHeader();
        var responseTopic = getString(header.properties(), MqttProperties.MqttPropertyType.AUTHENTICATION_METHOD);
        var authData = getBytes(header.properties(), MqttProperties.MqttPropertyType.AUTHENTICATION_DATA);
        var userProperty = (List<MqttProperties.StringProperty>) header.properties().getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        return new AuthPair(responseTopic, authData, header.reasonCode(), userProperty);
    }


    @SuppressWarnings("unchecked")
    private DisconnectPair parserDisconnectPair(MqttMessage mqttMessage) {
        var header = (MqttReasonCodeAndPropertiesVariableHeader) mqttMessage.variableHeader();
        var sessionExpiry = getInt(properties, MqttProperties.MqttPropertyType.SESSION_EXPIRY_INTERVAL);
        var userProperty = (List<MqttProperties.StringProperty>) header.properties().getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        return new DisconnectPair(header.reasonCode(), sessionExpiry, userProperty);
    }


    @SuppressWarnings("unchecked")
    private UnSubPair parserUnSubscribePair(MqttUnsubscribeMessage unsubscribeMessage) {
        var header = unsubscribeMessage.idAndPropertiesVariableHeader();
        var userProperty = (List<MqttProperties.StringProperty>) header.properties().getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        return new UnSubPair(userProperty);
    }

    @SuppressWarnings("unchecked")
    private SubPair parserSubscribePair(MqttSubscribeMessage unsubscribeMessage) {
        var header = unsubscribeMessage.idAndPropertiesVariableHeader();
        var subIdentifier = getInt(header.properties(), MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER);
        var userProperty = (List<MqttProperties.StringProperty>) header.properties().getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        return new SubPair(subIdentifier, userProperty);
    }

    @SuppressWarnings("unchecked")
    private AckPair parserAckPair(MqttMessage mqttMessage) {
        var header = (MqttPubReplyMessageVariableHeader) mqttMessage.variableHeader();
        var reason = getString(header.properties(), MqttProperties.MqttPropertyType.REASON_STRING);
        var userProperty = (List<MqttProperties.StringProperty>) header.properties().getProperties(
                MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        return new AckPair(reason, userProperty);
    }

    @SuppressWarnings("unchecked")
    private PublishPair parserPublishPair(MqttPublishMessage publishMessage) {
        final MqttProperties publishProperties = publishMessage.variableHeader().properties();
        byte payloadFormatIndicator = (byte) getInt(publishProperties, MqttProperties.MqttPropertyType.PAYLOAD_FORMAT_INDICATOR);
        int publicationExpiryInterval = getInt(publishProperties, MqttProperties.MqttPropertyType.PUBLICATION_EXPIRY_INTERVAL);
        int topicAlias = getInt(publishProperties, MqttProperties.MqttPropertyType.TOPIC_ALIAS);
        String responseTopic = getString(publishProperties, MqttProperties.MqttPropertyType.RESPONSE_TOPIC);
        byte[] correlationData = getBytes(publishProperties, MqttProperties.MqttPropertyType.CORRELATION_DATA);
        List<MqttProperties.StringProperty> userProperty = (List<MqttProperties.StringProperty>) publishProperties.getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        return new PublishPair(payloadFormatIndicator, publicationExpiryInterval, topicAlias,
                responseTopic, correlationData, userProperty);
    }


    private int getInt(MqttProperties properties, MqttProperties.MqttPropertyType type) {
        MqttProperties.IntegerProperty property =
                (MqttProperties.IntegerProperty) properties.getProperty(
                        type.value());
        return property != null ? property.value() : 0;
    }

    private String getString(MqttProperties properties, MqttProperties.MqttPropertyType type) {
        MqttProperties.StringProperty property =
                (MqttProperties.StringProperty) properties.getProperty(
                        type.value());
        return property != null ? property.value() : null;
    }

    private byte[] getBytes(MqttProperties properties, MqttProperties.MqttPropertyType type) {
        MqttProperties.BinaryProperty property =
                (MqttProperties.BinaryProperty) properties.getProperty(
                        type.value());
        return property != null ? property.value() : null;
    }


    @Override
    public void writeMessage(PublishPacket publishPacket) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false,
                MqttQoS.valueOf(publishPacket.getQos()), publishPacket.isRetain(), 0);
        MqttPublishVariableHeader variableHeader;
        if (properties != null)
            variableHeader = new MqttPublishVariableHeader(publishPacket.getTopic(), publishPacket.messageId(),
                    publishPacket.getMqttProperties());
        else
            variableHeader = new MqttPublishVariableHeader(publishPacket.getTopic(), publishPacket.messageId());

        connection.outbound()
                .sendObject(Mono.just(new MqttPublishMessage(mqttFixedHeader, variableHeader,
                        PooledByteBufAllocator.DEFAULT.directBuffer().writeBytes(publishPacket.getPayload())))
                )
                .then()
                .doOnSuccess(VOID -> {
                    mqttContext.getLogger().printInfo(String.format("write pub success  %s %s %s %d %s ",
                            publishPacket.endpoint().getClientId(),
                            publishPacket.endpoint().getClientIp(), "qos" + publishPacket.getQos(), publishPacket.messageId(),
                            HexFormat.of().formatHex(publishPacket.getPayload())));
                })
                .doOnError(throwable -> {
                    mqttContext.getLogger().printError(String.format("write error success  %s  %s  %s %d %s", publishPacket.endpoint().getClientId(),
                            publishPacket.endpoint().getClientIp(), "qos" + publishPacket.getQos(), publishPacket.messageId(),
                            HexFormat.of().formatHex(publishPacket.getPayload())), throwable);
                })
                .subscribe();
        if (publishPacket.isRetry()) {
            publishPacket.setRetry(false);
            publishPacket.setDup(true);
            this.doRetry(publishPacket);
        }
    }


    public void writeConnectAck(MqttConnectReturnCode connectReturnCode, MqttProperties properties) {
        mqttContext.getLogger().printInfo(String.format("write connect ack success  %s %s ",
                this.getClientId(),
                this.getClientIp()));
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(connectReturnCode, false);
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0X02);
        connection.outbound()
                .sendObject(Mono.just(new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader)))
                .then()
                .subscribe();
    }

    @Override
    public void writePublishAck(int messageId, byte reason, MqttProperties mqttProperties) {
        mqttContext.getLogger().printInfo(String.format("write pub ack success  %s %s %d ",
                this.getClientId(),
                this.getClientIp(), messageId));
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK,
                false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        MqttPubReplyMessageVariableHeader from = new MqttPubReplyMessageVariableHeader(messageId,
                reason, mqttProperties);
        connection.outbound()
                .sendObject(Mono.just(new MqttPubAckMessage(mqttFixedHeader, from)))
                .then()
                .subscribe();
    }


    @Override
    public void writePublishRec(PublishRecPacket publishRecPacket) {
        mqttContext.getLogger().printInfo(String.format("write pub rec success  %s %s %d ",
                this.getClientId(),
                this.getClientIp(), publishRecPacket.messageId()));
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        MqttPubReplyMessageVariableHeader from = new MqttPubReplyMessageVariableHeader(publishRecPacket.messageId(),
                publishRecPacket.getReason(), publishRecPacket.getMqttProperties());
        connection.outbound()
                .sendObject(Mono.just(new MqttPubAckMessage(mqttFixedHeader, from)))
                .then()
                .subscribe();
        if (publishRecPacket.isRetry()) {
            publishRecPacket.setRetry(false);
            publishRecPacket.setDup(true);
            this.doRetry(publishRecPacket);
        }
    }

    @Override
    public void writePublishRel(PublishRelPacket publishRelPacket) {
        mqttContext.getLogger().printInfo(String.format("write pub rel success  %s %s %d ",
                this.getClientId(),
                this.getClientIp(), publishRelPacket.messageId()));
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        MqttPubReplyMessageVariableHeader from = new MqttPubReplyMessageVariableHeader(publishRelPacket.messageId(),
                publishRelPacket.getReason(), publishRelPacket.getMqttProperties());
        connection.outbound()
                .sendObject(Mono.just(new MqttPubAckMessage(mqttFixedHeader, from)))
                .then()
                .subscribe();
        if (publishRelPacket.isRetry()) {
            publishRelPacket.setRetry(false);
            publishRelPacket.setDup(true);
            this.doRetry(publishRelPacket);
        }
    }

    private boolean doRetry(Packet packet) {
        var retryManager = mqttContext.getRetryManager();
        var retryMessage = new RetryMessage(packet.endpoint().getClientId(), packet.optCode(), packet.messageId());
        return retryManager.doRetry(new RetryTask<>(retryManager, retryMessage
                , packet,
                packet.endpoint().getMqttConfig().retrySize(),
                packet.endpoint().getMqttConfig().retryInterval()));
    }

    @Override
    public void writePublishComp(PublishCompPacket publishCompPacket) {
        mqttContext.getLogger().printInfo(String.format("write publish comp success  %s %s %d ",
                this.getClientId(),
                this.getClientIp(), publishCompPacket.messageId()));
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        MqttPubReplyMessageVariableHeader from = new MqttPubReplyMessageVariableHeader(publishCompPacket.messageId(),
                publishCompPacket.reason(), publishCompPacket.getMqttProperties());
        connection.outbound()
                .sendObject(Mono.just(new MqttPubAckMessage(mqttFixedHeader, from)))
                .then()
                .subscribe();
    }


    @Override
    public void writeSubAck(int messageId, List<Integer> responseCode, MqttProperties properties) {
        mqttContext.getLogger().printInfo(String.format("write sub ack success  %s %s %d ",
                this.getClientId(),
                this.getClientIp(), messageId));
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdAndPropertiesVariableHeader variableHeader = new MqttMessageIdAndPropertiesVariableHeader(messageId, properties);

        MqttSubAckPayload payload = new MqttSubAckPayload(responseCode);
        this.sendObject(new MqttSubAckMessage(mqttFixedHeader, variableHeader, payload));
    }

    @Override
    public void writeUnsubAck(int messageId, MqttProperties properties) {
        mqttContext.getLogger().printInfo(String.format("write unsub ack success  %s %s %d ",
                this.getClientId(),
                this.getClientIp(), messageId));
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        MqttMessageIdAndPropertiesVariableHeader variableHeader = new MqttMessageIdAndPropertiesVariableHeader(messageId, properties);
        this.sendObject(new MqttUnsubAckMessage(mqttFixedHeader, variableHeader));
    }

    @Override
    public void writeDisconnect(byte reasonCode, MqttProperties properties) {
        mqttContext.getLogger().printInfo(String.format("write disconnect success  %s %s %d ",
                this.getClientId(),
                this.getClientIp(), (int) reasonCode));
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        MqttReasonCodeAndPropertiesVariableHeader variableHeader = new MqttReasonCodeAndPropertiesVariableHeader(reasonCode, properties);
        this.sendObject(new MqttMessage(mqttFixedHeader, variableHeader));

    }


    private void sendObject(MqttMessage mqttMessage) {
        connection.outbound()
                .sendObject(Mono.just(mqttMessage))
                .then()
                .subscribe();
    }

    @Override
    public void writePong() {
        mqttContext.getLogger().printInfo(String.format("write pong success  %s %s  ",
                this.getClientId(),
                this.getClientIp()));
        this.sendObject(new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP,
                false, MqttQoS.AT_MOST_ONCE, false, 0)));
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
