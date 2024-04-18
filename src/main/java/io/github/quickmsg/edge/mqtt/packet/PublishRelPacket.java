package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.AckPair;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */

public record PublishRelPacket(Endpoint<Packet> endpoint, int messageId, byte reason, long timestamp,
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

    @Override
    public int optCode() {
        return 0;
    }

}
