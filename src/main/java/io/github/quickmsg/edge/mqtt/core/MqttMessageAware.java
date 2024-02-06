package io.github.quickmsg.edge.mqtt.core;

import io.github.quickmsg.edge.mqtt.Packet;
import io.github.quickmsg.edge.mqtt.MessageAware;
import io.github.quickmsg.edge.mqtt.packet.PublishPacket;
import io.github.quickmsg.edge.mqtt.packet.SubscribePacket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * @author luxurong
 */
public class MqttMessageAware implements MessageAware<Packet,Void> {

    @Override
    public Mono<Void> doHandler(Publisher<Packet> r) {
        return Mono.from(r)
                .doOnNext(this::doHandler)
                .then();
    }

    public void doHandler(Packet packet){
        switch (packet){
            case PublishPacket publishPacket->{}
            case SubscribePacket subscribePacket->{}
            default -> {}
        }


    }
}
