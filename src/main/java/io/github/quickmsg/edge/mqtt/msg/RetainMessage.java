package io.github.quickmsg.edge.mqtt.msg;

import io.github.quickmsg.edge.mqtt.packet.PublishPacket;
import io.github.quickmsg.edge.mqtt.pair.PublishPair;

import java.util.Objects;

/**
 * @author luxurong
 */
public class RetainMessage implements RetainExpire {

    private long createTime;
    private long expireTime;
    private String clientId;
    private String clientIp;
    private int messageId;
    private final String topic;
    private int qos;
    private byte[] payload;
    private boolean retain;
    private boolean dup;
    private boolean retry;
    private long timestamp;
    private PublishPair pair;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetainMessage that = (RetainMessage) o;
        return Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic);
    }

    public RetainMessage(long createTime,long expireTime, String clientId, String clientIp, int messageId, String topic,
                         int qos, byte[] payload, boolean retain, boolean dup,
                         boolean retry, long timestamp, PublishPair pair) {
        this.createTime = createTime;
        this.expireTime = expireTime;
        this.clientId = clientId;
        this.clientIp = clientIp;
        this.messageId = messageId;
        this.topic = topic;
        this.qos = qos;
        this.payload = payload;
        this.retain = retain;
        this.dup = dup;
        this.retry = retry;
        this.timestamp = timestamp;
        this.pair = pair;
    }


    public RetainMessage(String topic) {
        this.topic = topic;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public String getTopic() {
        return topic;
    }


    public String getClientId() {
        return clientId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public int getMessageId() {
        return messageId;
    }


    public int getQos() {
        return qos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isRetain() {
        return retain;
    }

    public boolean isDup() {
        return dup;
    }

    public boolean isRetry() {
        return retry;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PublishPair getPair() {
        return pair;
    }

    @Override
    public long expireTime() {
        return 0;
    }

    public PublishPacket toPacket() {
        return new PublishPacket(null, messageId, topic, qos, payload, retain, dup, retry, timestamp, pair);
    }
}
