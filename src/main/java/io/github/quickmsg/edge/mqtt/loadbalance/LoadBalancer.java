package io.github.quickmsg.edge.mqtt.loadbalance;

import java.util.List;

/**
 * @author luxurong
 */
public interface LoadBalancer<T> {
    T select(List<T> t, Object param);
}
