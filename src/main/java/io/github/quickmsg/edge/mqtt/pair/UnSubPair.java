package io.github.quickmsg.edge.mqtt.pair;

import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.List;

/**
 * @author luxurong
 * @param userProperty 用户属性 多个key-value类型
 */
public record UnSubPair(List<MqttProperties.StringPair> userProperty) {
}
