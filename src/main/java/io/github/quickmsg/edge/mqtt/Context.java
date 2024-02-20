package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.config.MqttConfig;
import io.github.quickmsg.edge.mqtt.log.AsyncLogger;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author luxurong
 */
public interface Context {
    MqttConfig getMqttConfig();
    Flux<Packet> start();

    Map<String,MqttAcceptor> getMqttAcceptors();


    TopicRegistry getTopicRegistry();


    EndpointRegistry getChannelRegistry();

    Authenticator getAuthenticator();

    AsyncLogger getLogger();


}
