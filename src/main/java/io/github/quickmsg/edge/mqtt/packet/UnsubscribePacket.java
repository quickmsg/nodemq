package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.UnSubPair;
import io.netty.handler.codec.mqtt.MqttProperties;

import java.util.Map;
import java.util.Set;

/**
 * @author luxurong
 */
public record UnsubscribePacket(Endpoint<Packet> endpoint, int messageId, Map<String,Boolean> topics,
                                long timestamp, UnSubPair unSubPair) implements Packet{


    @Override
    public MqttProperties getMqttProperties() {
        if(endpoint.isMqtt5()){
            // todo unsubscribeAck properties
            MqttProperties mqttProperties = new MqttProperties();
            return mqttProperties;

        }else{
            return MqttProperties.NO_PROPERTIES;
        }
    }
}
