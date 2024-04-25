package io.github.quickmsg.edge.mqtt.process;

import io.github.quickmsg.edge.mqtt.*;
import io.github.quickmsg.edge.mqtt.msg.RetainMessage;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.pair.AckPair;
import io.github.quickmsg.edge.mqtt.retry.RetryMessage;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

/**
 * @author luxurong
 */
public record MqttProcessor(MqttContext context) implements Processor {


    @Override
    public Mono<Void> processConnect(ConnectPacket packet) {
        return Mono.fromRunnable(() -> {

            var endpoint = packet.endpoint();
            boolean auth = context.getAuthenticator().auth(endpoint.getClientId(),
                    packet.connectUserDetail().username(),
                    packet.connectUserDetail().password());
            if (!auth) {
                context().getLogger().printInfo(String.format("connect failed  %s %s  ", packet.endpoint().getClientId(),
                        packet.endpoint().getClientIp()));
                if (endpoint.isMqtt5()) {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD, packet.getMqttProperties());
                } else {
                    endpoint.writeConnectAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, packet.getMqttProperties());
                }
            } else {
                endpoint.setWillMessage(packet.willMessage());
                context().getLogger().printInfo(String.format("connect success  %s %s  ", packet.endpoint().getClientId(),
                        packet.endpoint().getClientIp()));
                endpoint.setClientId(endpoint.getClientId());
                endpoint.setClosed(false);
                endpoint.setConnected(true);
                endpoint.onClose(() -> this.clearEndpoint(endpoint).subscribe());
                endpoint.readIdle(packet.keepalive() * 1000L, () -> {
                    endpoint.setCloseCode(3);
                    endpoint.close();
                });
                var oldEndpoint = context.getChannelRegistry().registry(endpoint);
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
            var endpoint = packet.endpoint();
            context().getLogger().printInfo(String.format("read pub  %s %s %s %d %s ", packet.endpoint().getClientId(),
                    packet.endpoint().getClientIp(), "qos" + packet.getQos(), packet.messageId(),
                    HexFormat.of().formatHex(packet.getPayload())));
            this.doRetain(packet);
            if(endpoint!=null && !endpoint.isClosed()){
                switch (packet.getQos()) {
                    case 1 -> {
                        if (context.getRetryManager().checkOverLimit()) {
                            endpoint.writePublishAck(packet.messageId(),
                                    MqttReasonCodes.PubAck.QUOTA_EXCEEDED.byteValue(), packet.getMqttProperties());
                            return;
                        } else {
                            endpoint.writePublishAck(packet.messageId(), (byte) 0, packet.getMqttProperties());
                        }
                    }
                    case 2 -> {
                        PublishRecPacket publishRecPacket;
                        if (!context.getRetryManager().checkOverLimit()) {
                            AckPair ackPair = null;
                            if (endpoint.isMqtt5()) {
                                ackPair = new AckPair(null, packet.getPair().userProperty());
                            }
                            if (endpoint.cacheQos2Message(packet)) {
                                publishRecPacket = new PublishRecPacket(packet.endpoint(), packet.messageId(),
                                        (byte) 0, System.currentTimeMillis(), false, true, ackPair);
                            } else {
                                publishRecPacket = new PublishRecPacket(packet.endpoint(), packet.messageId(),
                                        MqttReasonCodes.PubRec.PACKET_IDENTIFIER_IN_USE.byteValue(),
                                        System.currentTimeMillis(), false, true, ackPair);
                            }
                            endpoint.writePublishRec(publishRecPacket);
                        } else {
                            AckPair ackPair = null;
                            if (endpoint.isMqtt5()) {
                                ackPair = new AckPair(null, packet.getPair().userProperty());
                            }
                            publishRecPacket = new PublishRecPacket(packet.endpoint(), packet.messageId(),
                                    MqttReasonCodes.PubRec.QUOTA_EXCEEDED.byteValue(), System.currentTimeMillis(), false, false, ackPair);
                            endpoint.writePublishRec(publishRecPacket);
                        }
                        return;
                    }
                    default -> {
                    }
                }
            }
            this.sendMessage(packet);
        });

    }

    private void doRetain(PublishPacket packet) {
        if (packet.getPayload().length == 0) {
            context.getRetainStore().del(packet.getTopic());
        }
        if (packet.isRetain()) {
            context.getRetainStore().add(packet.getTopic(),
                    new RetainMessage(System.currentTimeMillis(), context.getMqttConfig().system().maxRetainExpiryInterval(),
                            packet.endpoint().getClientId(),
                            packet.endpoint().getClientId(),
                            packet.messageId(),
                            packet.getTopic(),
                            packet.getQos(),
                            packet.getPayload(),
                            true,
                            false,
                            packet.isRetry(),
                            packet.getTimestamp(),
                            packet.getPair()
                    ));
        }
    }

    private void sendMessage(PublishPacket packet) {
        var topicRegistry = context.getTopicRegistry();
        var channelRegistry = context.getChannelRegistry();
        var subscribeTopics = topicRegistry.searchTopicSubscribe(packet.getTopic());
        if (subscribeTopics != null && !subscribeTopics.isEmpty()) {
            Map<String, List<SubscribeTopic>> shareSubscribeTopic = new HashMap<>();
            for (var subscribeTopic : subscribeTopics) {
                if (!subscribeTopic.share()) {
                    var subscribeEndpoint = channelRegistry.getEndpoint(subscribeTopic.clientId());
                    subscribeEndpoint.writeMessage(
                            new PublishPacket(subscribeEndpoint,
                                    subscribeEndpoint.generateMessageId(), packet.getTopic(),
                                    Math.min(packet.getQos(), subscribeTopic.qos()), packet.getPayload(),
                                    packet.isRetain(), false, packet.isRetry(), System.currentTimeMillis(),
                                    packet.getPair()));
                } else {
                    var shareSubscribeGroup = shareSubscribeTopic.computeIfAbsent(subscribeTopic.topic(),
                            topic -> new LinkedList<>());
                    shareSubscribeGroup.add(subscribeTopic);
                }
            }
            if (!shareSubscribeTopic.isEmpty()) {
                shareSubscribeTopic.values()
                        .forEach(subscribeTopicList -> {
                            var select =
                                    context().getLoadBalancer().select(subscribeTopicList, packet.endpoint().getClientId());
                            var shareEndpoint = channelRegistry.getEndpoint(select.clientId());
                            shareEndpoint.writeMessage(new PublishPacket(shareEndpoint,
                                    shareEndpoint.generateMessageId(), "$share/" + packet.getTopic()
                                    , Math.min(packet.getQos(), select.qos()), packet.getPayload(),
                                    packet.isRetain(), false, packet.isRetry(), System.currentTimeMillis(),
                                    packet.getPair()));
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
                    context().getLogger().printInfo(String.format("read sub  %s %s %s %s %d", packet.endpoint().getClientId(),
                            packet.endpoint().getClientIp(), subscribeTopic.topic(), "qos" + subscribeTopic.qos(), packet.messageId()));
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
                        var retainStore = context().getRetainStore();
                        retainStore.get(subscribeTopic.topic())
                                .ifPresent(retainMessage -> {
                                    if ((retainMessage.getExpireTime() + retainMessage.getCreateTime())
                                            < System.currentTimeMillis()) {
                                        retainStore.del(retainMessage.getTopic());
                                    } else {
                                        var ps = retainMessage.toPacket(packet.endpoint());
                                        packet.endpoint()
                                                .writeMessage(ps);
                                    }
                                });
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
                    context().getLogger().printInfo(String.format("read unsub  %s %s %s ", clientId,
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
            context().getLogger().printInfo(String.format("read disconnect  %s %s ", packet.clientId(),
                    packet.clientIp()));
            packet.endpoint().close();
        });
    }

    @Override
    public Mono<Void> processPublishAck(PublishAckPacket packet) {
        return Mono.fromRunnable(() -> {
            context().getLogger().printInfo(String.format("pub ack  %s %s %d ", packet.endpoint().getClientId(),
                    packet.endpoint().getClientIp(), packet.messageId()));
            context().getRetryManager().cancelRetry(new RetryMessage(packet.endpoint().getClientId(), 0,
                    packet.messageId()));
        });
    }

    @Override
    public Mono<Void> processPublishRel(PublishRelPacket packet) {
        return Mono.fromRunnable(() -> {
            context().getLogger().printInfo(String.format("read pub rel  %s %s %d ", packet.endpoint().getClientId(),
                    packet.endpoint().getClientIp(), packet.messageId()));
            var cancelRetry = context().getRetryManager()
                    .cancelRetry(new RetryMessage(packet.endpoint().getClientId(), 1, packet.messageId()));
            if (cancelRetry != null) {
                packet.endpoint().writePublishComp(new PublishCompPacket(packet.endpoint()
                        , packet.messageId(), (byte) 0,
                        packet.getTimestamp(), packet.getAckPair()));
                var publishPacket = packet.endpoint().removeQos2Message(packet.messageId());
                if (publishPacket != null) {
                    this.sendMessage(publishPacket);
                }
            } else {
                packet.endpoint().writePublishComp(new PublishCompPacket(packet.endpoint()
                        , packet.messageId(), MqttReasonCodes.PubRel.PACKET_IDENTIFIER_NOT_FOUND.byteValue(),
                        packet.getTimestamp(), packet.getAckPair()));
            }

        });
    }


    @Override
    public Mono<Void> processPublishRec(PublishRecPacket packet) {
        return Mono.fromRunnable(() -> {


            var cancelRetry =
                    context().getRetryManager().cancelRetry(new RetryMessage(packet.endpoint().getClientId(), 0,
                            packet.messageId()));
            byte reason = cancelRetry != null ? (byte) 0 : MqttReasonCodes.PubRec.PACKET_IDENTIFIER_IN_USE.byteValue();
            packet.endpoint().writePublishRel(new PublishRelPacket(packet.endpoint()
                            , packet.messageId(), reason, packet.getTimestamp(), false, true, packet.getAckPair()));

        });
    }

    @Override
    public Mono<Void> processPublishComp(PublishCompPacket packet) {
        return Mono.fromRunnable(() -> {
            context().getLogger().printInfo(String.format("read pub comp  %s %s %d ", packet.endpoint().getClientId(),
                    packet.endpoint().getClientIp(), packet.messageId()));
            context().getRetryManager().cancelRetry(
                    new RetryMessage(packet.endpoint().getClientId(), 0, packet.messageId()));
        });
    }

    @Override
    public Mono<Void> processAuth(AuthPacket packet) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> processPing(PingPacket pingPacket) {
        return Mono.fromRunnable(() -> {
            context().getLogger().printInfo(String.format("read ping  %s %s  ", pingPacket.clientId(),
                    pingPacket.clientIp()));
            pingPacket.endpoint().writePong();
        });
    }

    @Override
    public Mono<Void> processClose(ClosePacket closePacket) {
        return Mono.defer(() -> {
            context().getLogger().printInfo(String.format("close  %s %s  ", closePacket.endpoint().getClientId(),
                    closePacket.endpoint().getClientIp()));
            closePacket.endpoint().setClosed(true);
            context.getChannelRegistry().remove(closePacket.endpoint());
            var msg  = closePacket.endpoint().getWillMessage();
            if(msg!=null){
                var pubPackage = new PublishPacket(closePacket.endpoint(),0,msg.willTopic(),msg.willQos(),
                        msg.willMessage(), msg.isRetain(), false,msg.willQos()>0,System.currentTimeMillis(),null);
                return this.processPublish(pubPackage);
            }
            var subscribeTopics = closePacket.endpoint().getSubscribeTopics();
            for (SubscribeTopic subscribeTopic : subscribeTopics) {
                context.getTopicRegistry().removeTopicSubscribe(subscribeTopic.topic(), subscribeTopic);
            }
            return Mono.empty();
        });
    }

}
