package io.github.quickmsg.edge.mqtt.config;

import io.github.quickmsg.edge.mqtt.loadbalance.Strategy;
import io.netty.handler.ssl.SslContext;

import java.util.List;

/**
 * @author luxurong
 */
public record InitConfig(List<MqttConfig> mqtt,HttpConfig http, SystemConfig system, LogConfig log) {
    public static InitConfig defaultConfig() {
        return new InitConfig(
                List.of(new MqttConfig("0.0.0.0", 1883, 65535,
                        3, false, null, false, "/mqtt", false,
                        2,
                        true, true,
                        10, 2000)),
                new HttpConfig(8080,false,null),
                new SystemConfig(Strategy.RANDOM,
                        1000, 1000, 7 * 24 * 60 * 60, 100),
                new LogConfig("INFO", false));
    }


    public record SystemConfig(Strategy shareStrategy,
                               int maxSessionMessageSize,
                               int maxRetainMessageSize,
                               long maxRetainExpiryInterval,
                               int unConfirmFlightWindowSize) {

    }

    public record HttpConfig(Integer port, 
                             boolean accessLog,
                             SslConfig ssl) {

    }


    public record MqttConfig(String host,
                             int port,
                             int maxMessageSize,
                             int connectTimeout,
                             boolean wiretap,
                             SslConfig ssl,
                             boolean useWebsocket,
                             String websocketPath,
                             boolean proxy,
                             int maxQosLevel,
                             boolean supportWildcardSubscribe,
                             boolean supportShareSubscribe,
                             int retrySize,
                             int retryInterval


    ) {
    }


    public record LogConfig(String level,
                            boolean persisted) {
    }

    public record SslConfig(String key, String crt, String ca) {

    }
}
