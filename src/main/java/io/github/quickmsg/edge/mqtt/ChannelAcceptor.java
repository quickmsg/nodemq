package io.github.quickmsg.edge.mqtt;

import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

/**
 * @author luxurong
 */
public interface ChannelAcceptor {


    String id();

    Flux<Endpoint<Packet>> accept();


    void close();


}
