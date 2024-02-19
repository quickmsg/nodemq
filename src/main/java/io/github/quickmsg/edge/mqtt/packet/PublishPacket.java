package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
/**
 * @author luxurong
 */


public record PublishPacket(MqttEndpoint endpoint, int messageId, String clientId, String clientIp,
                            String topic, int qos, byte[] payload, boolean retain, long timestamp) implements Packet {



}
