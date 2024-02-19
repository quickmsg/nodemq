package io.github.quickmsg.edge.mqtt.topic;

import io.github.quickmsg.edge.mqtt.TopicRegistry;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author luxurong
 */
public class MqttTopicRegistry implements TopicRegistry {

    private final TopicTreeNode<SubscribeTopic> subscribeTree;


    public MqttTopicRegistry() {
        this.subscribeTree =  new TopicTreeNode<>(new CopyOnWriteArraySet<>(),"root");
    }


    @Override
    public Set<SubscribeTopic> searchTopicSubscribe(String topic) {
        return subscribeTree.getObjectsByTopic(topic);
    }

    @Override
    public void addTopicSubscribe(String topicFilter, SubscribeTopic subscribeTopic) {
        subscribeTree.addObjectTopic(topicFilter,subscribeTopic);
    }

    @Override
    public void removeTopicSubscribe(String topicFilter, SubscribeTopic subscribeTopic) {
        subscribeTree.removeObjectTopic(topicFilter,subscribeTopic);
    }
}
