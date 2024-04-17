package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.AckPair;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */

public record PublishRelPacket(MqttEndpoint endpoint, int messageId, long timestamp,
                               AckPair ackPair) implements Packet {


    @Override
    public MqttProperties getMqttProperties() {
        if(endpoint.isMqtt5()){
            // todo publishtAck properties
            MqttProperties mqttProperties = new MqttProperties();
            return mqttProperties;

        }else{
            return MqttProperties.NO_PROPERTIES;
        }
    }
}
