package io.github.quickmsg.edge.mqtt.core;

/**
 * @author luxurong
 */
public record SubscribeTopic(String clientId,String topic,int qos) {
}
