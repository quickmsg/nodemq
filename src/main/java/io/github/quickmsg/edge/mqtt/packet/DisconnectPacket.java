package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.DisconnectPair;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */

public record DisconnectPacket(Endpoint<Packet> endpoint, String clientId, String clientIp, long timestamp, DisconnectPair disconnectPair) implements Packet{


    @Override
    public MqttProperties getMqttProperties() {
        return MqttProperties.NO_PROPERTIES;
    }
}
