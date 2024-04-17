package io.github.quickmsg.edge.mqtt.endpoint;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.config.BootstrapConfig;
import io.github.quickmsg.edge.mqtt.pair.*;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.github.quickmsg.edge.mqtt.util.MessageUtils;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.mqtt.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author luxurong
 */

public class MqttEndpoint implements Endpoint<Packet> {


    private static final Pattern SHARE_MESSAGE_PATTERN = Pattern.compile("\\$share/(.*)");

    private final Connection connection;
    private String clientId;

    private final String clientIp;

    private MqttVersion version;

    private MqttProperties properties;

    private MqttProperties willProperties;


    private volatile boolean connected;

    private final long connectTime;

    private volatile boolean closed;

    private volatile boolean keepSession;


    private final BootstrapConfig.MqttConfig mqttConfig;


    private transient AtomicInteger atomicInteger;

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


    /**
     * 0  client close
     * 1  server close
     * 2  kicked
     * 3  beats-kill
     * 4  disconnect
     */
    private volatile int closeCode = 0;


    private volatile int receiveMaxMessageSize;


    private final List<SubscribeTopic> subscribeTopics;

    public String getClientIp() {
        return clientIp;
    }

    public MqttEndpoint(BootstrapConfig.MqttConfig mqttConfig, Connection connection) {
        this.mqttConfig = mqttConfig;
        this.connection = connection;
        this.clientIp = connection.channel().remoteAddress().toString().split(":")[0];
        this.connectTime = System.currentTimeMillis();
        this.subscribeTopics = new ArrayList<>();
        this.readIdle(this.mqttConfig.connectTimeout(), this::close);
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

    public void setConnected(boolean connected) {
        this.connected = connected;
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
    public BootstrapConfig.MqttConfig getMqttConfig() {
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
    public void onClose(Runnable close) {
        connection.onDispose(close::run);
    }

    private Packet transfer(MqttMessage mqttMessage) {
        System.out.println("test");
        return switch (mqttMessage.fixedHeader().messageType()) {
            case CONNECT -> {
                var connectMessage = (MqttConnectMessage) mqttMessage;
                ConnectPacket.ConnectUserDetail userDetail = null;
                if (connectMessage.variableHeader().hasUserName() || connectMessage.variableHeader().hasPassword()) {
                    userDetail = new ConnectPacket.ConnectUserDetail(connectMessage.payload().userName(),
                            connectMessage.payload().passwordInBytes());
                }
                this.version = MqttVersion.fromProtocolNameAndLevel(connectMessage.variableHeader().name(), (byte) connectMessage.variableHeader().version());
                ConnectPacket.ConnectWillMessage willMessage = null;
                if (connectMessage.variableHeader().isWillFlag()) {
                    willMessage = new ConnectPacket.ConnectWillMessage(connectMessage.variableHeader().isWillRetain(),
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
                        willPair);
            }
            case PUBLISH -> {
                var publishMessage = (MqttPublishMessage) mqttMessage;
                var pair = this.isMqtt5() ? this.parserPublishPair(publishMessage) : null;
                yield new PublishPacket(this, publishMessage.variableHeader().packetId(),
                        publishMessage.variableHeader().topicName(), publishMessage.fixedHeader().qosLevel().value(),
                        MessageUtils.readByteBuf(publishMessage.payload()), publishMessage.fixedHeader().isRetain(),
                        false,true, System.currentTimeMillis(), pair);

            }

            case PUBACK -> {
                var ackPair = this.isMqtt5() ? this.parserAckPair(mqttMessage) : null;
                yield new PublishAckPacket(this,
                        ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId()
                        , System.currentTimeMillis(), ackPair);
            }
            case PUBREC -> {
                var ackPair = this.isMqtt5() ? this.parserAckPair(mqttMessage) : null;
                yield new PublishRecPacket(this, ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId()
                        , System.currentTimeMillis(), ackPair);
            }
            case PUBREL -> {
                var ackPair = this.isMqtt5() ? this.parserAckPair(mqttMessage) : null;
                yield new PublishRelPacket(this, ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(),
                       System.currentTimeMillis(), ackPair);
            }

            case PUBCOMP -> {
                var ackPair = this.isMqtt5() ? this.parserAckPair(mqttMessage) : null;
                yield new PublishCompPacket(this, ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(),
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
                                    var matcher =SHARE_MESSAGE_PATTERN.matcher(mqttTopicSubscription.topicFilter());
                                    SubscribeTopic subscribeTopic;
                                    if (matcher.find()) {
                                        subscribeTopic = new SubscribeTopic(clientId,matcher.group(1),
                                                mqttTopicSubscription.qualityOfService().value(),true);
                                    }
                                    else{
                                        subscribeTopic =new SubscribeTopic(clientId,mqttTopicSubscription.topicFilter(),
                                                mqttTopicSubscription.qualityOfService().value(),false);
                                    }
                                    return subscribeTopic;

                                })
                                .collect(Collectors.toSet());
                var subPair = this.isMqtt5() ? this.parserSubscribePair(subscribeMessage) : null;
                yield new SubscribePacket(this,subscribeMessage.variableHeader().messageId(), subscribeInfos,
                        System.currentTimeMillis(), subPair);
            }
            case UNSUBSCRIBE -> {
                var unsubscribeMessage = (MqttUnsubscribeMessage) mqttMessage;
                var unSubPair = this.isMqtt5() ? this.parserUnSubscribePair(unsubscribeMessage) : null;
                Map<String,Boolean> topics = new HashMap<>();
                for(String topicFilter: unsubscribeMessage.payload().topics()){
                    var matcher =SHARE_MESSAGE_PATTERN.matcher(topicFilter);
                    if (matcher.find()) {
                        topics.put(matcher.group(1),true);
                    }
                    else{
                        topics.put(topicFilter,false);
                    }
                }
                yield new UnsubscribePacket(this,unsubscribeMessage.idAndPropertiesVariableHeader().messageId(),
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
        var userProperty = (List<MqttProperties.StringProperty>) header.properties().getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
        return new AckPair(header.reasonCode(), userProperty);
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
    public void writeMessage(PublishPacket publishPacket, MqttProperties properties) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false,
                MqttQoS.valueOf(publishPacket.qos()), publishPacket.retain(), 0);
        MqttPublishVariableHeader variableHeader;
        if (properties != null)
            variableHeader = new MqttPublishVariableHeader(publishPacket.topic(),this.generateMessageId(), properties);
        else
            variableHeader = new MqttPublishVariableHeader(publishPacket.topic(),this.generateMessageId());
        connection.outbound()
                .sendObject(Mono.just(new MqttPublishMessage(mqttFixedHeader, variableHeader,
                                PooledByteBufAllocator.DEFAULT.directBuffer().writeBytes(publishPacket.payload())))
                        .then()
                        .subscribe());
    }

    public void writeConnectAck(MqttConnectReturnCode connectReturnCode, MqttProperties properties) {
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(connectReturnCode, false);
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0X02);
        connection.outbound()
                .sendObject(Mono.just(new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader)))
                .then()
                .subscribe();
    }

    @Override
    public void writeMessageAck(int messageId, MqttMessageType messageType, MqttProperties properties) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(messageType, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        MqttMessageIdAndPropertiesVariableHeader from = new MqttMessageIdAndPropertiesVariableHeader(messageId, properties);
        connection.outbound()
                .sendObject(Mono.just(new MqttPubAckMessage(mqttFixedHeader, from)))
                .then()
                .subscribe();
    }

    @Override
    public void writeSubAck(int messageId, List<Integer> responseCode,MqttProperties properties) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdAndPropertiesVariableHeader variableHeader  = new MqttMessageIdAndPropertiesVariableHeader(messageId,properties);

        MqttSubAckPayload payload = new MqttSubAckPayload(responseCode);
        this.sendObject(new MqttSubAckMessage(mqttFixedHeader, variableHeader, payload));


    }

    @Override
    public void writeUnsubAck(int messageId, MqttProperties properties) {

    }

    @Override
    public void writeDisconnect(MqttProperties properties) {

    }


    private void sendObject(MqttMessage mqttMessage){
        connection.outbound()
                .sendObject(Mono.just(mqttMessage))
                .then()
                .subscribe();
    }

    @Override
    public void writePong() {

    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
