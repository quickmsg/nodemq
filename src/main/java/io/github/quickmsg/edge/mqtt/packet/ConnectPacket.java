package io.github.quickmsg.edge.mqtt.packet;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.pair.ConnectPair;
import io.github.quickmsg.edge.mqtt.pair.WillPair;
import io.netty.handler.codec.mqtt.MqttVersion;

/**
 * @author luxurong
 */

public record ConnectPacket(MqttEndpoint endpoint,
                            String clientId,
                            String clientIp,
                            ConnectUserDetail connectUserDetail,
                            ConnectWillMessage willMessage,
                            boolean cleanSession,
                            MqttVersion version,
                            int keepalive,
                            long timestamp,
                            ConnectPair connectPair,
                            WillPair willPair) implements Packet {

    public record ConnectUserDetail(String username, byte[] password) {

    }


    public record ConnectWillMessage(boolean isRetain, String willTopic, int  qos, byte[] willMessage){}

    ;
}
