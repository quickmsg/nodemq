package io.github.quickmsg.edge.mqtt.core.pair;


import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.List;

/**
 * @author luxurong
 * @param willDelayInterval 发送延迟
 * @param payloadFormatIndicator 0 : 表示字節 1：utf-8
 * @param messageExpiryInterval 表示遺囑消息的延迟发布（秒）  0表示立即发送
 * @param contextType 一个UTF-8编码的字符串，用于描述遗嘱消息的内容
 * @param responseTopic 一个UTF-8编码字符串，该字符串用作响应消息的主题名称。
 * @param correlationData 二进制数据。请求消息的发送者使用相关数据来识别响应消息在接收时用于哪个请求。
 * @param userProperty 用户属性 多个key-value类型 发布遗嘱消息[MQTT-3.1.3.10]时，服务器必须维护用户属性的顺序。
 */
public record WillPair(int willDelayInterval, int payloadFormatIndicator,
                       int messageExpiryInterval, String contextType,
                       String responseTopic, byte[] correlationData, List<MqttProperties.StringProperty> userProperty) {
}
