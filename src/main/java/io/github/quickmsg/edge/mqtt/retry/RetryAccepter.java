package io.github.quickmsg.edge.mqtt.retry;

/**
 * @author luxurong
 */
@FunctionalInterface
public interface RetryAccepter<K,M> {

    void accept(RetryTask<K,M> retryTask);

}
