package io.github.quickmsg.edge.mqtt.retry;


/**
 * @author luxurong
 */
public interface RetryManager<K,M> {

   boolean  checkOverLimit();

   boolean doRetry(RetryTask<K,M> retryTask);


   M getRetryAttach(K k);

   M cancelRetry(K k);

   void retry(RetryTask<K,M> retryTask);



}
