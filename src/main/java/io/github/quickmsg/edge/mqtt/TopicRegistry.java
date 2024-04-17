package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;

import java.util.Set;

/**
 * @author luxurong
 */
public interface TopicRegistry {


    Set<SubscribeTopic> searchTopicSubscribe(String topic);


    boolean addTopicSubscribe(String topicFilter, SubscribeTopic subscribeTopic);


    void removeTopicSubscribe(String topicFilter, SubscribeTopic subscribeTopic);


}
