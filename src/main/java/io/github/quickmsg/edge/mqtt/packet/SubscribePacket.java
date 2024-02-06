package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Context;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.core.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.core.SubscribeTopic;

import java.util.Set;

/**
 * @author luxurong
 */

public record SubscribePacket(MqttEndpoint endpoint, String clientId, String clientIp, Set<SubscribeTopic>  subscribeTopics, long timestamp)
        implements Packet {


}
