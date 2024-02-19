package io.github.quickmsg.edge.mqtt.endpoint;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.pair.ConnectPair;
import io.github.quickmsg.edge.mqtt.pair.WillPair;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.github.quickmsg.edge.mqtt.util.MessageUtils;
import io.netty.handler.codec.mqtt.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author luxurong
 */

public class MqttEndpoint implements Endpoint<Packet> {


    private final Connection connection;
    private final String clientId;

    private final String clientIp;

    private MqttVersion version;

    private MqttProperties properties;

    private MqttProperties willProperties;


    private volatile boolean connected;

    private final long connectTime;

    public MqttEndpoint(Connection connection, String clientId) {
        this.connection = connection;
        this.clientId = clientId;
        this.clientIp = connection.channel().remoteAddress().toString().split(":")[0];
        this.connectTime =System.currentTimeMillis();
    }

    public void setConnected(boolean connected){
        this.connected = connected;
    }

    @Override
    public Mono<Void> write(Packet message) {
        return null;
    }

    @Override
    public Flux<Packet> receive() {
        return connection
                .inbound()
                .receive()
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
    public String getClientId() {
        return this.clientId;
    }

    private Packet transfer(MqttMessage mqttMessage) {
        return switch (mqttMessage.fixedHeader().messageType()) {
            case CONNECT -> {
                MqttConnectMessage connectMessage = (MqttConnectMessage) mqttMessage;
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
                ConnectPair connectPair = this.isMqtt5() ? this.parserConnectPair(this.properties) : null;
                WillPair willPair = this.isMqtt5() ? this.parserWillPair(this.willProperties) : null;
                connection.onReadIdle(2000,this::delayClose);
                yield new ConnectPacket(this,this.clientId, this.clientIp,
                        userDetail, willMessage,
                        connectMessage.variableHeader().isCleanSession(),
                        this.version,
                        connectMessage.variableHeader().keepAliveTimeSeconds(),
                        System.currentTimeMillis(),
                        connectPair,
                        willPair);
            }
            case PUBLISH -> {
                MqttPublishMessage publishMessage = (MqttPublishMessage) mqttMessage;
                yield new PublishPacket(this,publishMessage.variableHeader().packetId(), this.clientId, this.clientIp,
                        publishMessage.variableHeader().topicName(), publishMessage.fixedHeader().qosLevel().value(),
                        MessageUtils.readByteBuf(publishMessage.payload()), publishMessage.fixedHeader().isRetain(), System.currentTimeMillis());

            }

            case PUBACK ->
                    new PublishAckPacket(this,((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(), this.clientId, this.clientIp, System.currentTimeMillis());
            case PUBREC ->
                    new PublishRecPacket(this,((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(), this.clientId, this.clientIp, System.currentTimeMillis());
            case PUBREL ->
                    new PublishRelPacket(this,((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(), this.clientId, this.clientIp, System.currentTimeMillis());
            case PUBCOMP ->
                    new PublishCompPacket(this,((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId(), this.clientId, this.clientIp, System.currentTimeMillis());
            case SUBSCRIBE -> {
                MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) mqttMessage;
                Set<SubscribeTopic> subscribeInfos =
                        subscribeMessage
                                .payload()
                                .topicSubscriptions()
                                .stream()
                                .map(mqttTopicSubscription -> new SubscribeTopic(clientId, mqttTopicSubscription.topicName(),
                                        mqttTopicSubscription.qualityOfService().value()))
                                .collect(Collectors.toSet());
                yield new SubscribePacket(this,this.clientId, this.clientIp, subscribeInfos, System.currentTimeMillis());
            }
            case UNSUBSCRIBE -> {
                MqttUnsubscribeMessage unsubscribeMessage = (MqttUnsubscribeMessage) mqttMessage;

                yield new UnsubscribePacket(this,clientId, clientIp, new HashSet<>(unsubscribeMessage.payload().topics()), System.currentTimeMillis());
            }
            case DISCONNECT -> new DisconnectPacket(this,clientId, clientIp, System.currentTimeMillis());
            case PINGREQ -> new PingPacket(this,clientId, clientIp, System.currentTimeMillis());
            case AUTH -> new AuthPacket(this,clientId, clientIp, System.currentTimeMillis());
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
        List<MqttProperties.StringProperty> userProperty = (List<MqttProperties.StringProperty>) properties.getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
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


    public void writeConnectAck(MqttConnectReturnCode connectReturnCode) {
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(connectReturnCode, false);
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0X02);
        connection.outbound()
                .sendObject(Mono.just(new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader)))
                .then()
                .subscribe();
    }
}
