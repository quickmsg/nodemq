package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.pair.AckPair;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */

public class PublishRecPacket implements Packet{

    private final Endpoint<Packet> endpoint;
    private final int messageId;
    private final byte reason;

    private final long timestamp;

    private boolean dup;
    private  boolean retry;
    private final AckPair ackPair;

    public PublishRecPacket(Endpoint<Packet> endpoint, int messageId, byte reason, long timestamp, boolean dup, boolean retry, AckPair ackPair) {
        this.endpoint = endpoint;
        this.messageId = messageId;
        this.reason = reason;
        this.timestamp = timestamp;
        this.dup = dup;
        this.retry = retry;
        this.ackPair = ackPair;
    }

    public boolean isDup() {
        return dup;
    }

    public void setDup(boolean dup) {
        this.dup = dup;
    }


    public byte getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public AckPair getAckPair() {
        return ackPair;
    }

    @Override
    public Endpoint<Packet> endpoint() {
        return this.endpoint;
    }

    @Override
    public MqttProperties getMqttProperties() {
        if(endpoint.isMqtt5()){
            // todo publishtAck properties
            MqttProperties mqttProperties = new MqttProperties();
            return mqttProperties;

        }else{
            return MqttProperties.NO_PROPERTIES;
        }
    }

    @Override
    public int messageId() {
        return this.messageId;
    }

    @Override
    public int optCode() {
        return 1;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }


}
