package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.AuthPair;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */

public record AuthPacket(MqttEndpoint endpoint, String clientId, String clientIp, long timestamp, AuthPair authPair) implements Packet {


    @Override
    public MqttProperties getMqttProperties() {
        return MqttProperties.NO_PROPERTIES;
    }
}
