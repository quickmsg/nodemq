package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.github.quickmsg.edge.mqtt.msg.WillMessage;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.proxy.ProxyMessage;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttProperties;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author luxurong
 */
public interface Endpoint<M> {

    WillMessage getWillMessage();

    void setWillMessage(WillMessage willMessage);


    ProxyMessage getProxyMessage();

    int generateMessageId();

    void writeMessage(PublishPacket publishPacket);

    void writePublishAck(int messageId, byte reason, MqttProperties mqttProperties);

    void writeConnectAck(MqttConnectReturnCode connectReturnCode, MqttProperties properties);

    void writePublishRec(PublishRecPacket publishRecPacket);

    void writePublishRel(PublishRelPacket publishRelPacket);

    void writePublishComp(PublishCompPacket publishCompPacket);

    void writeSubAck(int messageId, List<Integer> responseCode, MqttProperties properties);

    void writeUnsubAck(int messageId, MqttProperties properties);

    void writeDisconnect(byte reasonCode, MqttProperties properties);

    void writePong();

    List<SubscribeTopic> getSubscribeTopics();

    Flux<M> receive();


    boolean isMqtt5();

    boolean isWriteable();


    MqttProperties connectProperties();

    MqttProperties willProperties();

    long connectTime();

    InitConfig.MqttConfig getMqttConfig();

    String getClientId();

    String getClientIp();

    boolean connected();

    void setConnected(boolean connected);


    boolean isClosed();

    void setClosed(boolean closed);

    void readIdle(long keeps, Runnable runnable);

    void writeIdle(long keeps, Runnable runnable);

    void readWriteIdle(long keeps, Runnable runnable);

    void onClose(Runnable runnable);


    void close();

    boolean cacheQos2Message(PublishPacket packet);


    PublishPacket removeQos2Message(int messageId);


    PublishPacket getQos2Message(int messageId);

    void setClientId(String clientId);

    void setCloseCode(int i);

    int getCloseCode();
}
