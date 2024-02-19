package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;

/**
 * @author luxurong
 */

public record AuthPacket(MqttEndpoint endpoint, String clientId, String clientIp, long timestamp) implements Packet {



}
