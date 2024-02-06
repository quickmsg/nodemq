package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Context;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.core.MqttEndpoint;

import java.util.Set;

/**
 * @author luxurong
 */
public record UnsubscribePacket(MqttEndpoint endpoint, String clientId, String clientIp, Set<String> topics, long timestamp) implements Packet{



}
