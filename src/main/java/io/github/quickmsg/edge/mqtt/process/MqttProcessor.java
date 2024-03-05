package io.github.quickmsg.edge.mqtt.process;

import io.github.quickmsg.edge.mqtt.EndpointRegistry;
import io.github.quickmsg.edge.mqtt.MqttContext;
import io.github.quickmsg.edge.mqtt.Processor;
import io.github.quickmsg.edge.mqtt.TopicRegistry;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * @author luxurong
 */
public record MqttProcessor(MqttContext context) implements Processor {


    @Override
    public Mono<Void> processConnect(ConnectPacket packet) {
        return Mono.fromRunnable(() -> {
            final MqttEndpoint endpoint = packet.endpoint();
            boolean auth = context.getAuthenticator().auth(packet.clientId(),
                    packet.connectUserDetail().username(),
                    packet.connectUserDetail().password());
            if (!auth) {
                if (endpoint.isMqtt5()) {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD);
                } else {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                }
            } else {
                endpoint.setConnected(true);
                endpoint.onClose(() -> this.clearEndpoint(endpoint));
                endpoint.readIdle(packet.keepalive(), () -> {
                    endpoint.setCloseCode(3);
                    endpoint.close();
                });
                if (endpoint.isMqtt5()) {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_ACCEPTED);
                } else {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_ACCEPTED);
                }
            }
        });
    }

    private void clearEndpoint(MqttEndpoint endpoint) {
        context.getLogger().printInfo(String.format(" %s %s  reason: %s ", endpoint.getClientIp(), endpoint.getClientId(), endpoint.getCloseCode()));
        context.getChannelRegistry().remove(endpoint);
        final List<SubscribeTopic> subscribeTopics = endpoint.getSubscribeTopics();
        for (SubscribeTopic subscribeTopic : subscribeTopics) {
            context.getTopicRegistry().removeTopicSubscribe(subscribeTopic.topic(), subscribeTopic);
        }
    }

    @Override
    public Mono<Void> processPublish(PublishPacket packet) {
        return Mono.fromRunnable(() -> {
            final var endpoint = packet.endpoint();
            final var topicRegistry = context.getTopicRegistry();
            final var channelRegistry = context.getChannelRegistry();
            final var subscribeTopics = topicRegistry.searchTopicSubscribe(packet.topic());
            if (subscribeTopics != null && !subscribeTopics.isEmpty()) {
                for (var subscribeTopic : subscribeTopics) {
//                    endpoint.write(packet);
                }
            }
        });

    }

    @Override
    public Mono<Void> processSubscribe(SubscribePacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processUnSubscribe(UnsubscribePacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processDisconnect(DisconnectPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processPublishAck(PublishAckPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processPublishRel(PublishRelPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processPublishRec(PublishRecPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processPublishComp(PublishCompPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processAuth(AuthPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processPing(PingPacket pingPacket) {
        return Mono.empty();
    }

    @Override
    public Mono<Object> processClose(ClosePacket closePacket) {
        return Mono.empty();
    }

}
