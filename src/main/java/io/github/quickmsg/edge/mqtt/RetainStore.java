package io.github.quickmsg.edge.mqtt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author luxurong
 */
public class RetainStore<V>{

    private final Map<String,V> cacheMap = new ConcurrentHashMap<>();

    public void add(String topic,V v){
        cacheMap.put(topic,v);
    }

    public   void del(String topic){
        cacheMap.remove(topic);
    }

    public V get(String topic){
       return  cacheMap.get(topic);
    }


}
