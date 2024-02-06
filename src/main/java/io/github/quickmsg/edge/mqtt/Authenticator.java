package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.packet.ConnectPacket;

import java.util.Map;

/**
 * @author luxurong
 */
public interface Authenticator {
    boolean auth(String clientId, String username, byte[] password);

}
