package io.github.quickmsg.edge.mqtt.pair;

import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.List;

/**
 *@author luxurong
 */
public record PublishPair(byte payloadFormatIndicator,int publicationExpiryInterval,int topicAlias,
                          String responseTopic, byte[] correlationData, List<MqttProperties.StringProperty> userProperty) {
}
