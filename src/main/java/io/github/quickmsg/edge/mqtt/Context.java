package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.github.quickmsg.edge.mqtt.loadbalance.LoadBalancer;
import io.github.quickmsg.edge.mqtt.log.AsyncLogger;
import io.github.quickmsg.edge.mqtt.packet.PublishPacket;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author luxurong
 */
public interface Context {
    InitConfig getMqttConfig();
    Flux<Packet> start();

    Map<String,MqttAcceptor> getMqttAcceptors();


    TopicRegistry getTopicRegistry();


    EndpointRegistry getChannelRegistry();

    Authenticator getAuthenticator();

    AsyncLogger getLogger();

    LoadBalancer<SubscribeTopic> getLoadBalancer();

    RetainStore<PublishPacket> getRetainStore();


}
