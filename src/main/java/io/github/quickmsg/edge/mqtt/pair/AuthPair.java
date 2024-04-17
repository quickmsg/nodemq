package io.github.quickmsg.edge.mqtt.pair;

import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.List;

/**
 * @author luxurong
 */
public record AuthPair(String authMethod, byte[] authData,byte reason, List<MqttProperties.StringProperty> userProperties) {
}
