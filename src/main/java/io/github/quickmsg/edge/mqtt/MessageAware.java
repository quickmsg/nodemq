package io.github.quickmsg.edge.mqtt;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * @author luxurong
 */
public interface MessageAware<R,O> {

    Mono<O> doHandler(Publisher<R> r);
}
