package io.github.quickmsg.edge.mqtt;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author luxurong
 */
public interface Endpoint<M> {

    void writeMessage(int messageId,String topic,int qos,byte[] payload,boolean retain);

    void writeConnectAck(MqttConnectReturnCode connectReturnCode);

    void writeMessageAck(int messageId);

    void writeSubAck(int messageId);

    void writeUnsubAck(int messageId);

    void writeDisconnect();
    void writePong();

    Flux<M> receive();


    boolean isMqtt5();

    boolean connected();


    MqttProperties connectProperties();

    MqttProperties willProperties();

    long connectTime();

    String getClientId();


    boolean isClosed();

    void readIdle(long keeps,Runnable runnable);

    void writeIdle(long keeps,Runnable runnable);

    void readWriteIdle(long keeps,Runnable runnable);

    void onClose(Runnable runnable);


    void close();

}
