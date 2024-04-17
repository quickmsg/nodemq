package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.github.quickmsg.edge.mqtt.packet.PublishPacket;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author luxurong
 */
public interface Endpoint<M> {

    void writeMessage(PublishPacket publishPacket,MqttProperties properties);

    void writeConnectAck(MqttConnectReturnCode connectReturnCode,MqttProperties properties);

    void writeMessageAck(int messageId,MqttMessageType messageType,MqttProperties properties);

    void writeSubAck(int messageId, List<Integer> responseCode, MqttProperties properties);

    void writeUnsubAck(int messageId,MqttProperties properties);

    void writeDisconnect(MqttProperties properties);
    void writePong();

    List<SubscribeTopic> getSubscribeTopics();

    Flux<M> receive();


    boolean isMqtt5();

    boolean connected();


    MqttProperties connectProperties();

    MqttProperties willProperties();

    long connectTime();

    InitConfig.MqttConfig getMqttConfig();

    String getClientId();

    String getClientIp();


    boolean isClosed();

    void readIdle(long keeps,Runnable runnable);

    void writeIdle(long keeps,Runnable runnable);

    void readWriteIdle(long keeps,Runnable runnable);

    void onClose(Runnable runnable);


    void close();

}
