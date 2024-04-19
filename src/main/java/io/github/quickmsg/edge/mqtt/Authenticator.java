package io.github.quickmsg.edge.mqtt;


/**
 * @author luxurong
 */
public interface Authenticator {
    boolean auth(String clientId, String username, byte[] password);

}
