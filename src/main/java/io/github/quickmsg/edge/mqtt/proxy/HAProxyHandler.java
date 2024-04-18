package io.github.quickmsg.edge.mqtt.proxy;

import io.github.quickmsg.edge.mqtt.util.MessageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyTLV;
import io.netty.util.AttributeKey;

import java.util.List;

/**
 * @author luxurong
 */
public class HAProxyHandler extends MessageToMessageDecoder<Object> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, Object msg, List<Object> list) throws Exception {
        if (msg instanceof HAProxyMessage haProxyMessage) {
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setSourceAddress(haProxyMessage.sourceAddress());
            proxyMessage.setSourcePort(haProxyMessage.sourcePort());
            proxyMessage.setDestinationAddress(haProxyMessage.destinationAddress());
            proxyMessage.setDestinationPort(haProxyMessage.destinationPort());
            for (HAProxyTLV haProxyTLV : haProxyMessage.tlvs()) {
                switch (haProxyTLV.type()) {
                    case PP2_TYPE_SSL -> {
                        try {
                            proxyMessage.setSsl(new String(MessageUtils.readByteBuf(haProxyTLV.content())));
                        }
                        catch (Exception e){
                            // ignore
                        }
                    }
                    case PP2_TYPE_SSL_CN -> {
                        try {
                            proxyMessage.setSslCn(new String(MessageUtils.readByteBuf(haProxyTLV.content())));
                        } catch (Exception e){
                            // ignore
                        }
                    }
                    case PP2_TYPE_SSL_VERSION -> {
                        try {
                            proxyMessage.setSslVersion(new String(MessageUtils.readByteBuf(haProxyTLV.content())));
                        }  catch (Exception e){
                            // ignore
                        }
                    }
                }
            }
            channelHandlerContext.channel().attr(AttributeKey.valueOf("proxy")).set(proxyMessage);
            channelHandlerContext.channel().pipeline().remove(this);
        } else if (msg instanceof ByteBuf) {
            channelHandlerContext.fireChannelRead(msg);
        }
    }
}
