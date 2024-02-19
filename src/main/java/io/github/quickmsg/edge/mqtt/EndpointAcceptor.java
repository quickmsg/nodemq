package io.github.quickmsg.edge.mqtt;

import reactor.core.publisher.Flux;

/**
 * @author luxurong
 */
public interface EndpointAcceptor {


    String id();

    Flux<Endpoint<Packet>> accept();


    void close();


}
