package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */

public record PingPacket(MqttEndpoint endpoint, String clientId, String clientIp, long timestamp) implements Packet {


    @Override
    public MqttProperties getMqttProperties() {
        return MqttProperties.NO_PROPERTIES;
    }
}
