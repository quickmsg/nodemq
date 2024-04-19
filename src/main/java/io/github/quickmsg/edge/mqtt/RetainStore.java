package io.github.quickmsg.edge.mqtt;

import com.github.benmanes.caffeine.cache.*;
import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.github.quickmsg.edge.mqtt.msg.RetainMessage;
import io.github.quickmsg.edge.mqtt.topic.TopicTreeNode;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * @author luxurong
 */
public class RetainStore{
    private final TopicTreeNode<RetainMessage> topicTreeNode
            = new TopicTreeNode<>(new CopyOnWriteArraySet<>(),"root");

    public RetainStore() {

    }

    public void add(String topic,RetainMessage retainMessage){
        topicTreeNode.removeObjectTopic(topic,retainMessage);
        topicTreeNode.addObjectTopic(topic,retainMessage);
    }

    public  void del(String topic){
        topicTreeNode.addObjectTopic(topic,new RetainMessage(topic));
    }

    public Optional<RetainMessage> get(String topic){
        var objects=  topicTreeNode.getObjectsByTopic(topic);
        return  objects.stream().findFirst();
    }


}
