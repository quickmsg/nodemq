package io.github.quickmsg.edge.mqtt.msg;

/**
 * Created by  lxr.
 * User: luxurong
 * Date: 2024/4/20
 */
public record WillMessage(boolean isRetain, String willTopic, int willQos, byte[] willMessage) {
}
