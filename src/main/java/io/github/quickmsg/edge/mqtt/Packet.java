package io.github.quickmsg.edge.mqtt;

import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */
public interface Packet {


    Endpoint<Packet> endpoint();


    MqttProperties getMqttProperties();




}
