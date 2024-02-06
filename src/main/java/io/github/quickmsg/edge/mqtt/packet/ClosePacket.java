package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;

/**
 * @author luxurong
 *
 */
public record ClosePacket(Endpoint<Packet> endpoint,int  closeCode,long timestamp) implements Packet {




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
