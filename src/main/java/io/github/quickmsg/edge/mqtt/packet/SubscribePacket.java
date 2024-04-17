package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.AckPair;
import io.github.quickmsg.edge.mqtt.pair.SubPair;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.Set;

/**
 * @author luxurong
 */

public record SubscribePacket(MqttEndpoint endpoint,int messageId, Set<SubscribeTopic>  subscribeTopics, long timestamp
        , SubPair subPair)
        implements Packet {


    @Override
    public MqttProperties getMqttProperties() {
        if(endpoint.isMqtt5()){
            // todo subscribeAck properties
            MqttProperties mqttProperties = new MqttProperties();
            return mqttProperties;

        }else{
            return MqttProperties.NO_PROPERTIES;
        }
    }
}
