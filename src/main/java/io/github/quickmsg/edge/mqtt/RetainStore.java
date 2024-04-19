package io.github.quickmsg.edge.mqtt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.github.quickmsg.edge.mqtt.msg.RetainMessage;
import org.checkerframework.checker.index.qual.NonNegative;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author luxurong
 */
public class RetainStore{
    private final Cache<String, RetainMessage> cacheMap = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, RetainMessage>() {
                @Override
                public long expireAfterCreate(String key, RetainMessage value, long currentTime) {
                    return currentTime+ TimeUnit.SECONDS.toNanos(value.expireTime());
                }

                @Override
                public long expireAfterUpdate(String key, RetainMessage value, long currentTime, @NonNegative long currentDuration) {
                    return 0;
                }

                @Override
                public long expireAfterRead(String key, RetainMessage value, long currentTime, @NonNegative long currentDuration) {
                    return 0;
                }
            })
            .build();



    public RetainStore() {

    }

    public void add(String topic,RetainMessage retainMessage){
        cacheMap.put(topic,retainMessage);
    }

    public  void del(String topic){
        cacheMap.invalidate(topic);
    }

    public RetainMessage get(String topic){
        return  cacheMap.getIfPresent(topic);
    }


}
