package io.github.quickmsg.edge.mqtt.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author luxurong
 */
public record MqttConfig(List<MqttItem> mqtt,SystemItem system,LogItem log) {
    public static MqttConfig defaultConfig() {
        return new MqttConfig(
                List.of(new MqttItem("0.0.0.0",1883,65535,
                        3,false,null,false,
                        2,1000,1000,
                        true,true)),
                        new SystemItem(),new LogItem("",false));
    }


    public record SystemItem(
                             ){}

    
    public record MqttItem(String host,
                           int port,
                           int maxMessageSize,
                           int connectTimeout,
                           boolean wiretap,
                           SslConfig sslConfig,
                           boolean useWebsocket,
                           int maxQosLevel,
                           int maxSessionMessageSize,
                           int maxRetainMessageSize,
                           boolean supportWildcardSubscribe,
                           boolean supportShareSubscribe
    ){}


    public record LogItem(String level,
                          boolean persisted){}

    public record SslConfig(String key,String crt,String ca) {

    }
}
