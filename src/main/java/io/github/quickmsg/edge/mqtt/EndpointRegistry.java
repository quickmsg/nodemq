package io.github.quickmsg.edge.mqtt;

/**
 * @author luxurong
 */
public interface EndpointRegistry {

    Endpoint<Packet> registry(Endpoint<Packet> endpoint);

    boolean remove(Endpoint<Packet> endpoint);

    Endpoint<Packet> getEndpoint(String clientId);


}
