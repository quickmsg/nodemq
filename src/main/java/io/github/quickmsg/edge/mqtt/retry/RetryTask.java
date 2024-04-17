package io.github.quickmsg.edge.mqtt.retry;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author luxurong
 */
public class RetryTask<K, M> implements TimerTask {

    private final TimeAckManager<K, M> timeAckManager;

    private int retryCount;

    private final int retrySize;

    private final int retryPeriod;

    private volatile boolean timeoutFlag = false;

    private Timeout timeout;

    private final M m;

    private final K k;


    public RetryTask(TimeAckManager<K, M> timeAckManager, K k, M m, int retrySize, int retryPeriod) {
        this.timeAckManager = timeAckManager;
        this.k = k;
        this.m = m;
        this.retrySize = retrySize;
        this.retryPeriod = retryPeriod;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        if (!timeout.isCancelled() && this.retryCount < retrySize) {
            timeAckManager.retry(this);
            retryCount++;
            this.timeout = timeout.timer().newTimeout(this, retryPeriod, TimeUnit.SECONDS);
        } else {
            if (!timeoutFlag) {
                timeAckManager.cancelRetry(k);
            }
        }
    }


    public void cancel() {
        Optional.ofNullable(timeout)
                .ifPresent(timeout -> this.timeoutFlag = timeout.cancel());

    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public int getRetryPeriod() {
        return retryPeriod;
    }

    public K getK() {
        return k;
    }

    public M getM() {
        return m;
    }
}
