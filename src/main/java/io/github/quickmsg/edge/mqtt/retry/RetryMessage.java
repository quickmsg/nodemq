package io.github.quickmsg.edge.mqtt.retry;


import java.util.Objects;

/**
 * @author luxurong
 */
public record RetryMessage(String clientId, int optCode,int messageId) {


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetryMessage that = (RetryMessage) o;
        return optCode == that.optCode && messageId == that.messageId && Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, optCode, messageId);
    }
}
