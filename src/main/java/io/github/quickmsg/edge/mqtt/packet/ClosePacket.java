package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * @author luxurong
 *
 */
public record ClosePacket(Endpoint<Packet> endpoint,int  closeCode,long timestamp) implements Packet {


    @Override
    public MqttProperties getMqttProperties() {
        return MqttProperties.NO_PROPERTIES;
    }

    public  enum CloseReason{

        CLOSE(0x01);

        private final int value;

        public int getValue() {
            return value;
        }

        CloseReason(int value) {
            this.value = value;
        }
    }

}
