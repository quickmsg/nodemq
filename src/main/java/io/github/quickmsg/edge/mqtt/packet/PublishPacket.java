package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.PublishPair;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 */


public record PublishPacket(MqttEndpoint endpoint, int messageId,
                            String topic, int qos, byte[] payload, boolean retain,boolean dup,boolean retry,
                            long timestamp, PublishPair pair) implements Packet {





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
