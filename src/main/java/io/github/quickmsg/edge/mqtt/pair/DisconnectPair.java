package io.github.quickmsg.edge.mqtt.pair;

import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.List;

/**
 * @author luxurong
 */
public record DisconnectPair(int sessionExpiryInterval,String reason, List<MqttProperties.StringPair> userProperty) {
}
