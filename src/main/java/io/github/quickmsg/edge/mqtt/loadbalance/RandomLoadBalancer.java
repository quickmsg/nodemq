package io.github.quickmsg.edge.mqtt.loadbalance;

import java.util.List;
import java.util.Random;

/**
 * @author luxurong
 */
public class RandomLoadBalancer<T> implements LoadBalancer<T> {

    private final Random random = new Random();

    @Override
    public T select(List<T> t, Object object) {
        int randomPos = random.nextInt(t.size());
        return t.get(randomPos);
    }
}