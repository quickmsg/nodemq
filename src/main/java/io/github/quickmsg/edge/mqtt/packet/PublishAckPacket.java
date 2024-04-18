package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.AckPair;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */

public record PublishAckPacket(MqttEndpoint endpoint, int messageId,byte reason, long timestamp,
                               AckPair ackPair) implements Packet {
    @Override
    public MqttProperties getMqttProperties() {
        return MqttProperties.NO_PROPERTIES;
    }
}
