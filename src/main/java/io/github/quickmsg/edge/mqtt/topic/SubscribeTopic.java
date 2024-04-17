package io.github.quickmsg.edge.mqtt.topic;

import java.util.Objects;

/**
 * @author luxurong
 */
public record SubscribeTopic(String clientId,String topic,int qos,boolean share) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscribeTopic that = (SubscribeTopic) o;
        return share == that.share && Objects.equals(clientId, that.clientId) && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, topic, share);
    }


    public boolean isWildcard(){
        return topic.endsWith("#") || topic.contains("+");
    }

}
