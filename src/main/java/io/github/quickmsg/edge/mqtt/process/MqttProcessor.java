package io.github.quickmsg.edge.mqtt.process;

import io.github.quickmsg.edge.mqtt.*;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.pair.AckPair;
import io.github.quickmsg.edge.mqtt.retry.RetryMessage;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * @author luxurong
 */
public record MqttProcessor(MqttContext context) implements Processor {


    @Override
    public Mono<Void> processConnect(ConnectPacket packet) {
        return Mono.fromRunnable(() -> {
            final Endpoint<Packet> endpoint = packet.endpoint();
            boolean auth = context.getAuthenticator().auth(endpoint.getClientId(),
                    packet.connectUserDetail().username(),
                    packet.connectUserDetail().password());
            if (!auth) {
                if (endpoint.isMqtt5()) {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD, packet.getMqttProperties());
                } else {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, packet.getMqttProperties());
                }
            } else {
                endpoint.setClientId(endpoint.getClientId());
                endpoint.setConnected(true);
                endpoint.onClose(() -> this.clearEndpoint(endpoint).subscribe());
                endpoint.readIdle(packet.keepalive() * 1000L, () -> {
                    endpoint.setCloseCode(3);
                    endpoint.close();
                });
                final Endpoint<Packet> oldEndpoint = context.getChannelRegistry().registry(endpoint);
                if (oldEndpoint != null) {
                    oldEndpoint.close();
                }
                if (endpoint.isMqtt5()) {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_ACCEPTED, packet.getMqttProperties());
                } else {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_ACCEPTED, packet.getMqttProperties());
                }
            }
        });
    }

    private Mono<Void> clearEndpoint(Endpoint<Packet> endpoint) {
        return this.processClose(new ClosePacket(endpoint, endpoint.getCloseCode(), System.currentTimeMillis())).then();
    }

    @Override
    public Mono<Void> processPublish(PublishPacket packet) {
        return Mono.fromRunnable(() -> {
            final var endpoint = packet.endpoint();
            switch (packet.qos()) {
                case 1 -> {
                    if(context.getRetryManager().checkOverLimit()){
                        endpoint.writePublishAck(packet.messageId(),
                                MqttReasonCodes.PubAck.QUOTA_EXCEEDED.byteValue(),packet.getMqttProperties());
                        return;
                    }
                    else{
                        endpoint.writePublishAck(packet.messageId(),(byte)0,packet.getMqttProperties());
                    }
                }
                case 2 -> {
                    PublishRecPacket publishRecPacket;
                    if(endpoint.cacheQos2Message(packet)){
                        publishRecPacket = new PublishRecPacket(packet.endpoint(),packet.messageId(),
                                (byte) 0,System.currentTimeMillis(),new AckPair(null,packet.pair().userProperty()));
                        endpoint.writePublishRec(publishRecPacket,true);
                    }
                    else {
                        //流量超过直接回复reason code 不在重试
                        publishRecPacket = new PublishRecPacket(packet.endpoint(),packet.messageId(),
                                MqttReasonCodes.PubRec.QUOTA_EXCEEDED.byteValue(),System.currentTimeMillis(),new AckPair(null,packet.pair().userProperty()));
                        endpoint.writePublishRec(publishRecPacket,false);
                        return;
                    }
                }
                default -> {
                }
            }
            this.sendMessage(packet);
        });

    }

    private void sendMessage(PublishPacket packet){
        var topicRegistry = context.getTopicRegistry();
        var channelRegistry = context.getChannelRegistry();
        var subscribeTopics = topicRegistry.searchTopicSubscribe(packet.topic());
        if (subscribeTopics != null && !subscribeTopics.isEmpty()) {
            Map<String, List<SubscribeTopic>> shareSubscribeTopic = new HashMap<>();
            for (var subscribeTopic : subscribeTopics) {
                if (!subscribeTopic.share()) {
                    var subscribeEndpoint = channelRegistry.getEndpoint(subscribeTopic.clientId());
                    subscribeEndpoint.writeMessage(
                            new PublishPacket(subscribeEndpoint,
                                    subscribeEndpoint.generateMessageId(), packet.topic(), packet.qos(), packet.payload(),
                                    packet.retain(),false,true,System.currentTimeMillis(),packet.pair()),true);
                } else {
                    var shareSubscribeGroup = shareSubscribeTopic.computeIfAbsent(subscribeTopic.topic(),
                            topic -> new LinkedList<>());
                    shareSubscribeGroup.add(subscribeTopic);
                }
            }
            if(!shareSubscribeTopic.isEmpty()){
                shareSubscribeTopic.values()
                        .forEach(subscribeTopicList->{
                            var select =
                                    context().getLoadBalancer().select(subscribeTopicList, packet.endpoint().getClientId());
                            var shareEndpoint = channelRegistry.getEndpoint(select.clientId());
                            shareEndpoint.writeMessage(new PublishPacket(shareEndpoint,
                                    shareEndpoint.generateMessageId(), packet.topic(), packet.qos(), packet.payload(),
                                    packet.retain(),false,true,System.currentTimeMillis(),packet.pair()),true);
                        });
            }

        }
    }

    @Override
    public Mono<Void> processSubscribe(SubscribePacket packet) {
        return Mono.fromRunnable(() -> {
            var subscribeTopics = packet.subscribeTopics();
            List<Integer> responseCodes = new ArrayList<>();
            if (subscribeTopics != null && !subscribeTopics.isEmpty()) {
                for (SubscribeTopic subscribeTopic : packet.subscribeTopics()) {
                    context().getLogger().printInfo(String.format("sub  %s %s %s %d", packet.endpoint().getClientId(),
                            packet.endpoint().getClientIp(), subscribeTopic.topic(), subscribeTopic.qos()));
                    if (subscribeTopic.share()) {
                        if (!packet.endpoint().getMqttConfig().supportShareSubscribe()) {
                            responseCodes.add((int) MqttReasonCodes.SubAck.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED.byteValue());
                            break;
                        }

                    }
                    if (subscribeTopic.isWildcard()) {
                        if (!packet.endpoint().getMqttConfig().supportWildcardSubscribe()) {
                            responseCodes.add((int) MqttReasonCodes.SubAck.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED.byteValue());
                            break;
                        }
                    }
                    if (subscribeTopic.topic().contains("#") && !subscribeTopic.topic().endsWith("#")) {
                        responseCodes.add((int) MqttReasonCodes.SubAck.TOPIC_FILTER_INVALID.byteValue());
                        break;
                    }
                    if (context().getTopicRegistry()
                            .addTopicSubscribe(subscribeTopic.topic(), subscribeTopic)) {
                        responseCodes.add(subscribeTopic.qos());
                    } else {
                        responseCodes.add((int) MqttReasonCodes.SubAck.PACKET_IDENTIFIER_IN_USE.byteValue());
                    }

                }
            }
            packet.endpoint()
                    .writeSubAck(packet.messageId(), responseCodes, packet.getMqttProperties());

        });
    }

    @Override
    public Mono<Void> processUnSubscribe(UnsubscribePacket packet) {
        return Mono.fromRunnable(() -> {
            var subscribeTopics = packet.topics();
            if (subscribeTopics != null && !subscribeTopics.isEmpty()) {
                for (Map.Entry<String, Boolean> topicEntry : subscribeTopics.entrySet()) {
                    var clientId = packet.endpoint().getClientId();
                    context().getLogger().printInfo(String.format("unsub  %s %s %s ", clientId,
                            packet.endpoint().getClientIp(), topicEntry.getValue() ?
                                    "$share/" + topicEntry.getKey() : topicEntry.getKey()));
                    context().getTopicRegistry()
                            .removeTopicSubscribe(topicEntry.getKey(),
                                    new SubscribeTopic(clientId, topicEntry.getKey(), 0, topicEntry.getValue()));
                }
            }
            packet.endpoint()
                    .writeUnsubAck(packet.messageId(), packet.getMqttProperties());

        });
    }

    @Override
    public Mono<Void> processDisconnect(DisconnectPacket packet) {
        return Mono.fromRunnable(() -> {
            context().getLogger().printInfo(String.format("disconnect  %s %s ", packet.clientId(),
                    packet.clientIp()));
            packet.endpoint().close();
        });
    }

    @Override
    public Mono<Void> processPublishAck(PublishAckPacket packet) {
        return Mono.fromRunnable(() -> {
            context().getRetryManager().cancelRetry(new RetryMessage(packet.endpoint().getClientId(), packet.messageId()));
        });
    }

    @Override
    public Mono<Void> processPublishRel(PublishRelPacket packet) {
        return Mono.fromRunnable(() -> {
            var cancelRetry = context().getRetryManager()
                    .cancelRetry(new RetryMessage(packet.endpoint().getClientId(), packet.messageId()));
            if(cancelRetry!=null){
                packet.endpoint().writePublishComp(new PublishCompPacket(packet.endpoint()
                        ,packet.messageId(),(byte)0,
                        packet.timestamp(),packet.ackPair()));
                var publishPacket = packet.endpoint().removeQos2Message(packet.messageId());
                if(publishPacket!=null){
                    this.sendMessage(publishPacket);
                }
            }
            else{
                packet.endpoint().writePublishComp(new PublishCompPacket(packet.endpoint()
                        ,packet.messageId(),MqttReasonCodes.PubRel.PACKET_IDENTIFIER_NOT_FOUND.byteValue(),
                        packet.timestamp(),packet.ackPair()));
            }

        });
    }


    @Override
    public Mono<Void> processPublishRec(PublishRecPacket packet) {
        return Mono.fromRunnable(() -> {
            var cancelRetry =
                    context().getRetryManager().cancelRetry(new RetryMessage(packet.endpoint().getClientId(),
                            packet.messageId()));
            byte reason = cancelRetry!=null ? (byte)0 : MqttReasonCodes.PubRel.PACKET_IDENTIFIER_NOT_FOUND.byteValue();
            packet.endpoint().writePublishRel( new PublishRelPacket(packet.endpoint()
                    ,packet.messageId(),reason,packet.timestamp(),packet.ackPair()),
                    true);

        });
    }

    @Override
    public Mono<Void> processPublishComp(PublishCompPacket packet) {
        return Mono.fromRunnable(() -> {
            context().getRetryManager().cancelRetry(
                    new RetryMessage(packet.endpoint().getClientId(), packet.messageId()));
        });
    }

    @Override
    public Mono<Void> processAuth(AuthPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processPing(PingPacket pingPacket) {
        return Mono.fromRunnable(() -> {
            context().getLogger().printInfo(String.format("ping  %s %s  ", pingPacket.clientId(),
                    pingPacket.clientIp()));
        });
    }

    @Override
    public Mono<Object> processClose(ClosePacket closePacket) {
        return Mono.fromRunnable(() -> {
            context().getLogger().printInfo(String.format("close  %s %s  ", closePacket.endpoint().getClientId(),
                    closePacket.endpoint().getClientIp()));
            context.getChannelRegistry().remove(closePacket.endpoint());
            final List<SubscribeTopic> subscribeTopics = closePacket.endpoint().getSubscribeTopics();
            for (SubscribeTopic subscribeTopic : subscribeTopics) {
                context.getTopicRegistry().removeTopicSubscribe(subscribeTopic.topic(), subscribeTopic);
            }
        });
    }

}
