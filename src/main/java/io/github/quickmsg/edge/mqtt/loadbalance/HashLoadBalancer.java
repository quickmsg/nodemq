package io.github.quickmsg.edge.mqtt.loadbalance;



import java.util.List;

/**
 * @author luxurong
 */
public class HashLoadBalancer<T> implements LoadBalancer<T> {

    @Override
    public T select(List<T> t, Object param) {
        int hashValue = param.hashCode();
        return t.get(hashValue % t.size());
    }
}