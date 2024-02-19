package io.github.quickmsg.edge.mqtt.pair;

import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.List;

/**
 * @author luxurong
 * @param subIdentifier
 * 后跟代表订阅标识符的可变字节整数。订阅标识符的值可以是 1 到 268,435,455。如果订阅标识符的值为 0，则为协议错误。多次包含订阅标识符为协议错误。
 * 订阅标识符与由于该 SUBSCRIBE 数据包而创建或修改的任何订阅相关联。如果存在订阅标识符，则它与订阅一起存储。如果未指定此属性，则订阅标识符的缺失将与订阅一起存储。
 * @param userProperty 用户属性 多个key-value类型
 */
public record SubPair(String subIdentifier, List<MqttProperties.StringPair> userProperty) {
}
