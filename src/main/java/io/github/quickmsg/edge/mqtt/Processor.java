package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.packet.*;
import reactor.core.CorePublisher;
import reactor.core.publisher.Mono;

/**
 * @author luxurong
 */
public interface Processor {

    Mono<Void> processConnect(ConnectPacket packet);
    Mono<Void> processPublish(PublishPacket packet);
    Mono<Void> processSubscribe(SubscribePacket packet);
    Mono<Void> processUnSubscribe(UnsubscribePacket packet);
    Mono<Void> processDisconnect(DisconnectPacket packet);
    Mono<Void> processPublishAck(PublishAckPacket packet);
    Mono<Void> processPublishRel(PublishRelPacket packet);
    Mono<Void> processPublishRec(PublishRecPacket packet);
    Mono<Void> processPublishComp(PublishCompPacket packet);
    Mono<Void> processAuth(AuthPacket packet);
    Mono<Void> processPing(PingPacket pingPacket);
    Mono<Object> processClose(ClosePacket closePacket);
}
