package io.github.quickmsg.edge.mqtt.auth;

import io.github.quickmsg.edge.mqtt.Authenticator;

import java.util.Map;

/**
 * @author luxurong
 */
public class MqttAuthenticator implements Authenticator {
    @Override
    public boolean auth(String clientId, String username, byte[] password) {
        return false;
    }
}
