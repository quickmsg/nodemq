package io.github.quickmsg.edge.mqtt.config;

import io.github.quickmsg.edge.mqtt.loadbalance.Strategy;

import java.util.List;

/**
 * @author luxurong
 */
public record BootstrapConfig(List<MqttConfig> mqtt, SystemConfig system, LogConfig log) {
    public static BootstrapConfig defaultConfig() {
        return new BootstrapConfig(
                List.of(new MqttConfig("0.0.0.0",1883,65535,
                        3,false,null,false,false,
                        2,1000,1000,
                        true,true)),
                        new SystemConfig(Strategy.RANDOM),new LogConfig("",false));
    }


    public record SystemConfig(Strategy strategy){

    }


    public record MqttConfig(String host,
                             int port,
                             int maxMessageSize,
                             int connectTimeout,
                             boolean wiretap,
                             SslConfig sslConfig,
                             boolean useWebsocket,
                             boolean proxy,
                             int maxQosLevel,
                             int maxSessionMessageSize,
                             int maxRetainMessageSize,
                             boolean supportWildcardSubscribe,
                             boolean supportShareSubscribe
    ){}


    public record LogConfig(String level,
                            boolean persisted){}

    public record SslConfig(String key,String crt,String ca) {

    }
}
