package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.PublishPair;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */


public class PublishPacket implements Packet {

    private final Endpoint<Packet> endpoint;
    private final int messageId;


    private final String topic;
    private final int qos;
    private final byte[] payload;
    private final boolean retain;
    private boolean dup;

    public void setRetry(boolean retry) {
        this.retry = retry;
    }
    private boolean retry;
    private final long timestamp;
    private final PublishPair pair;

    public PublishPacket(Endpoint<Packet> endpoint, int messageId, String topic,
                         int qos, byte[] payload, boolean retain, boolean dup, boolean retry, long timestamp, PublishPair pair) {
        this.endpoint = endpoint;
        this.messageId = messageId;
        this.topic = topic;
        this.qos = qos;
        this.payload = payload;
        this.retain = retain;
        this.dup = dup;
        this.retry = retry;
        this.timestamp = timestamp;
        this.pair = pair;
    }

    public void setDup(boolean dup) {
        this.dup = dup;
    }

    @Override
    public Endpoint<Packet> endpoint() {
        return this.endpoint;
    }

    @Override
    public int messageId() {
        return this.messageId;
    }

    public boolean isRetry() {
        return retry;
    }


    @Override
    public MqttProperties getMqttProperties() {
        if(endpoint!=null && endpoint.isMqtt5()){
            // todo publishtAck properties
            MqttProperties mqttProperties = new MqttProperties();
            return mqttProperties;

        }else{
            return MqttProperties.NO_PROPERTIES;
        }
    }




    public String getTopic() {
        return topic;
    }

    public int getQos() {
        return qos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isRetain() {
        return retain;
    }

    public boolean isDup() {
        return dup;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public PublishPair getPair() {
        return pair;
    }

    @Override
    public int optCode() {
        return 0;
    }
}
