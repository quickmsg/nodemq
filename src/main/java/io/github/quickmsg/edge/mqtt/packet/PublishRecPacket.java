package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.core.MqttEndpoint;

/**
 * @author luxurong
 */

public record PublishRecPacket(MqttEndpoint endpoint, int messageId, String clientId, String clientIp, long timestamp) implements Packet{




}
