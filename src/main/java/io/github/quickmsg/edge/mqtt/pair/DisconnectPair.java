package io.github.quickmsg.edge.mqtt.pair;

import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.List;

/**
 * @author luxurong
 */
public record DisconnectPair(byte reason,int sessionExpiryInterval, List<MqttProperties.StringProperty> userProperty) {
}
