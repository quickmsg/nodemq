package io.github.quickmsg.edge.mqtt.retry;

import io.netty.util.HashedWheelTimer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author luxurong
 */
public class TimeAckManager<K,M> extends HashedWheelTimer implements RetryManager<K,M> {

    private final Map<K, RetryTask<K,M>> retryMap = new ConcurrentHashMap<>();

    private final RetryAccepter<K,M> retryAccepter;

    public TimeAckManager(long tickDuration, TimeUnit unit, int ticksPerWheel,RetryAccepter<K,M> retryAccepter) {
        super(tickDuration, unit, ticksPerWheel);
        this.retryAccepter = retryAccepter;
    }

    @Override
    public void doRetry(RetryTask<K,M> retryTask) {
        retryTask.setTimeout(this.newTimeout(retryTask,retryTask.getRetryPeriod(),TimeUnit.SECONDS));
        retryMap.put(retryTask.getK(),retryTask);
    }

    @Override
    public M getRetryAttach(K k) {
        return Optional.ofNullable(retryMap.get(k)).map(RetryTask::getM).orElse(null);
    }

    @Override
    public M cancelRetry(K k) {
        var retryTask = retryMap.get(k);
        if(retryTask!=null){
            retryTask.cancel();
            return retryTask.getM();
        }
        return null;
    }

    @Override
    public void retry(RetryTask<K, M> retryTask) {
        this.retryAccepter.accept(retryTask);
    }
}
