package io.github.quickmsg.edge.mqtt.pair;


import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.List;

/**
 *@author luxurong
 * @param sessionExpiry  会话保持时间 0xFFFFFFFF：表示不过期  如果会话到期间隔大于0[MQTT-3.1.2-23]，则客户端和服务器必须在网络连接关闭后存储会话状态。
 * @param receiveMaximum  客户端使用此值来限制其愿意同时处理的QoS 1和QoS 2发布的数量。不限制服务器可能尝试发送的QoS 0发布的机制，默认是65535
 * @param maxPacketSize   数据包大小是MQTT控制数据包中的字节总数，如第2.1.4节所定义。客户端使用最大数据包大小通知服务器，它将不会处理超过此限制的数据包。
 * @param maxTopicAliasMaximum 单个连接最大的别名数
 * @param requestResponseInformation 客户端使用此值请求服务器返回CONNACK中的响应信息。值为0表示服务器不得返回响应信息。如果值为1，则服务器可以在CONNACK数据包中返回响应信息。
 * @param requestProblemInformation 客户端使用此值来指示在发生故障时是否发送原因字符串或用户属性。0 返回  1不复回
 * @param userProperty 用户属性 多个key-value类型
 * @param authMethod 认证方法
 * @param authData  认证数据
 *List<MqttProperties.StringProperty>
 *
 */
public record ConnectPair(int sessionExpiry, int receiveMaximum, int maxPacketSize, int maxTopicAliasMaximum,
                          int requestResponseInformation, int requestProblemInformation,
                          List<MqttProperties.StringProperty> userProperty,String authMethod, byte[] authData) {
}
