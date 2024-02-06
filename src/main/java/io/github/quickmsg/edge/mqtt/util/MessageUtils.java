package io.github.quickmsg.edge.mqtt.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttMessage;

/**
 * @author luxurong
 */
public class MessageUtils {

    public static void safeRelease(MqttMessage mqttMessage) {
        if (mqttMessage.payload() instanceof ByteBuf byteBuf) {
            int count = byteBuf.refCnt();
            if (count > 0) {
                byteBuf.release(count);
            }
        }
    }

    public static void safeRelease(MqttMessage mqttMessage, Integer count) {
        if (mqttMessage.payload() instanceof ByteBuf byteBuf) {
            if (count > 0) {
                byteBuf.release(count);
            }
        }
    }

    public static void safeRelease(ByteBuf buf) {
        int count = buf.refCnt();
        if (count > 0) {
            buf.release(count);
        }
    }

    public static void safeRelease(ByteBuf buf, Integer count) {
        if (count > 0) {
            buf.release(count);
        }
    }


    /**
     * 获取释放消息字节数组
     *
     * @param byteBuf 消息ByteBuf
     * @return 字节数组
     */
    public static byte[] copyReleaseByteBuf(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        byteBuf.resetReaderIndex();
        return bytes;
    }


    /**
     * 获取释放消息字节数组
     *
     * @param byteBuf 消息ByteBuf
     * @return 字节数组
     */
    public static byte[] copyByteBuf(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.resetReaderIndex();
        byteBuf.readBytes(bytes);
        byteBuf.resetReaderIndex();
        return bytes;
    }

    /**
     * 获取释放消息字节数组
     *
     * @param byteBuf 消息ByteBuf
     * @return 字节数组
     */
    public static byte[] readByteBuf(ByteBuf byteBuf) {
        int size = byteBuf.readableBytes();
        byte[] bytes = new byte[size];
        if (size > 0) {
            byteBuf.resetReaderIndex();
            byteBuf.readBytes(bytes);
            byteBuf.resetReaderIndex();
        }
        return bytes;
    }


}
