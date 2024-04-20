package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.msg.WillMessage;
import io.github.quickmsg.edge.mqtt.pair.ConnectPair;
import io.github.quickmsg.edge.mqtt.pair.WillPair;
import io.github.quickmsg.edge.mqtt.proxy.ProxyMessage;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttVersion;

/**
 * @author luxurong
 */

public record ConnectPacket(Endpoint<Packet> endpoint,
                            ConnectUserDetail connectUserDetail,
                            WillMessage willMessage,
                            boolean cleanSession,
                            MqttVersion version,
                            int keepalive,
                            long timestamp,
                            ConnectPair connectPair,
                            ProxyMessage proxyMessage,
                            WillPair willPair) implements Packet {

    @Override
    public MqttProperties getMqttProperties() {
        if(endpoint.isMqtt5()){
            // todo connectAck properties
            return null;
        }
        else{
            return MqttProperties.NO_PROPERTIES;
        }
    }

    public record ConnectUserDetail(String username, byte[] password) {

    }


    ;
}
