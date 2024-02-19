package io.github.quickmsg.edge.mqtt.endpoint;

import io.github.quickmsg.edge.mqtt.Endpoint;
import io.github.quickmsg.edge.mqtt.EndpointRegistry;
import io.github.quickmsg.edge.mqtt.Packet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author luxurong
 */
public class MqttEndpointRegistry implements EndpointRegistry {


    private final Map<String,Endpoint<Packet>> endpointMap;

    public MqttEndpointRegistry() {
        this.endpointMap = new ConcurrentHashMap<>();
    }

    @Override
    public Endpoint<Packet> registry(Endpoint<Packet> endpoint) {
       return endpointMap.put(endpoint.getClientId(),endpoint);
    }

    @Override
    public boolean remove(Endpoint<Packet> endpoint) {
        return endpointMap.remove(endpoint.getClientId(),endpoint);
    }

    @Override
    public Endpoint<Packet> getEndpoint(String clientId) {
        return endpointMap.get(clientId);
    }
}
