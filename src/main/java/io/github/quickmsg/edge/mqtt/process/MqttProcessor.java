package io.github.quickmsg.edge.mqtt.process;

import io.github.quickmsg.edge.mqtt.*;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.retry.RetryMessage;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
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
            boolean auth = context.getAuthenticator().auth(endpoint.getClientId(),
                    packet.connectUserDetail().username(),
                    packet.connectUserDetail().password());
            if (!auth) {
                if (endpoint.isMqtt5()) {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD,packet.getMqttProperties());
                } else {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD,packet.getMqttProperties());
                }
            } else {
                endpoint.setClientId(endpoint.getClientId());
                endpoint.setConnected(true);
                endpoint.onClose(()->this.clearEndpoint(endpoint).subscribe());

                endpoint.readIdle(packet.keepalive()* 1000L, () -> {
                    endpoint.setCloseCode(3);
                    endpoint.close();
                });
                final Endpoint<Packet> oldEndpoint = context.getChannelRegistry().registry(endpoint);
                if(oldEndpoint!=null){
                    oldEndpoint.close();
                }
                if (endpoint.isMqtt5()) {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_ACCEPTED,packet.getMqttProperties());
                } else {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_ACCEPTED,packet.getMqttProperties());
                }
            }
        });
    }

    private Mono<Void> clearEndpoint(MqttEndpoint endpoint) {
        return this.processClose(new ClosePacket(endpoint,endpoint.getCloseCode(),System.currentTimeMillis())).then();
    }

    @Override
    public Mono<Void> processPublish(PublishPacket packet) {
        return Mono.fromRunnable(() -> {
            final var endpoint = packet.endpoint();
            final var topicRegistry = context.getTopicRegistry();
            final var channelRegistry = context.getChannelRegistry();
            final var subscribeTopics = topicRegistry.searchTopicSubscribe(packet.topic());
            switch (packet.qos()){
                case 1->{
                    endpoint.writeMessageAck(packet.messageId(), MqttMessageType.PUBACK,packet.getMqttProperties());
                }
                case 2 ->{
                    endpoint.writeMessageAck(packet.messageId(), MqttMessageType.PUBREC,packet.getMqttProperties());
                    return;
                }
                default -> {}
            }
            if (subscribeTopics != null && !subscribeTopics.isEmpty()) {
                for (var subscribeTopic : subscribeTopics) {
                    var subscribeEndpoint = channelRegistry.getEndpoint(subscribeTopic.clientId());
                    subscribeEndpoint.writeMessage(packet,packet.getMqttProperties());
                }
            }
        });

    }

    @Override
    public Mono<Void> processSubscribe(SubscribePacket packet) {
        return Mono.fromRunnable(()->{
           var  subscribeTopics = packet.subscribeTopics();
            List<Integer> responseCodes = new ArrayList<>();
            if(subscribeTopics!=null && !subscribeTopics.isEmpty()){
               for(SubscribeTopic subscribeTopic: packet.subscribeTopics()){
                   context().getLogger().printInfo(String.format("sub  %s %s %s %d",packet.endpoint().getClientId(),
                           packet.endpoint().getClientIp(),subscribeTopic.topic(),subscribeTopic.qos()));
                   if(context().getTopicRegistry()
                           .addTopicSubscribe(subscribeTopic.topic(),subscribeTopic)){
                       responseCodes.add(subscribeTopic.qos());
                   }
                   else{
                       responseCodes.add((int)MqttReasonCodes.SubAck.PACKET_IDENTIFIER_IN_USE.byteValue());
                   }
               }
           }
            packet.endpoint()
                    .writeSubAck(packet.messageId(),responseCodes,packet.getMqttProperties());

        });
    }

    @Override
    public Mono<Void> processUnSubscribe(UnsubscribePacket packet) {
        return Mono.fromRunnable(()->{
            var  subscribeTopics = packet.topics();
            if(subscribeTopics!=null && !subscribeTopics.isEmpty()){
                for(String topic: subscribeTopics){
                    var clientId = packet.endpoint().getClientId();
                    context().getLogger().printInfo(String.format("unsub  %s %s %s ",clientId,
                            packet.endpoint().getClientIp(),topic));
                    context().getTopicRegistry()
                            .removeTopicSubscribe(topic,new SubscribeTopic(clientId, topic,0));
                }
            }
            packet.endpoint()
                    .writeUnsubAck(packet.messageId(),packet.getMqttProperties());

        });
    }

    @Override
    public Mono<Void> processDisconnect(DisconnectPacket packet) {
        return Mono.fromRunnable(()->{
            context().getLogger().printInfo(String.format("disconnect  %s %s ",packet.clientId(),
                    packet.clientIp()));
            packet.endpoint().close();
        });
    }

    @Override
    public Mono<Void> processPublishAck(PublishAckPacket packet) {
        return Mono.fromRunnable(()->{
            context().getRetryManager().cancelRetry(new RetryMessage(packet.endpoint().getClientId(),packet.messageId()));
        });
    }

    @Override
    public Mono<Void> processPublishRel(PublishRelPacket packet) {
        return Mono.fromRunnable(()->{
            context().getRetryManager().cancelRetry(new RetryMessage(packet.endpoint().getClientId(),packet.messageId()));
        });
    }

    @Override
    public Mono<Void> processPublishRec(PublishRecPacket packet) {
        return Mono.fromRunnable(()->{
            context().getRetryManager().cancelRetry(new RetryMessage(packet.endpoint().getClientId(),packet.messageId()));
        });
    }

    @Override
    public Mono<Void> processPublishComp(PublishCompPacket packet) {
        return Mono.fromRunnable(()->{
            context().getRetryManager().cancelRetry(new RetryMessage(packet.endpoint().getClientId(),packet.messageId()));
        });
    }

    @Override
    public Mono<Void> processAuth(AuthPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processPing(PingPacket pingPacket) {
        return Mono.fromRunnable(()->{
            context().getLogger().printInfo(String.format("ping  %s %s  ",pingPacket.clientId(),
                    pingPacket.clientIp()));
        });
    }

    @Override
    public Mono<Object> processClose(ClosePacket closePacket) {
        return Mono.fromRunnable(()->{
            context().getLogger().printInfo(String.format("close  %s %s  ",closePacket.endpoint().getClientId(),
                    closePacket.endpoint().getClientIp()));
            context.getChannelRegistry().remove(closePacket.endpoint());
            final List<SubscribeTopic> subscribeTopics = closePacket.endpoint().getSubscribeTopics();
            for (SubscribeTopic subscribeTopic : subscribeTopics) {
                context.getTopicRegistry().removeTopicSubscribe(subscribeTopic.topic(), subscribeTopic);
            }
        });
    }

}
