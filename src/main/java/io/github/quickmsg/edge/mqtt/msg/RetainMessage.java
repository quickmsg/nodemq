package io.github.quickmsg.edge.mqtt.msg;

import io.github.quickmsg.edge.mqtt.packet.PublishPacket;
import io.github.quickmsg.edge.mqtt.pair.PublishPair;

/**
 * @author luxurong
 */
public record RetainMessage(long expireTime, String clientId,String clientIp,int messageId,
                            String topic, int qos, byte[] payload, boolean retain, boolean dup, boolean retry,
                            long timestamp, PublishPair pair) implements RetainExpire {
    @Override
    public long expireTime() {
        return 0;
    }
}
