package io.github.quickmsg.edge.mqtt.config;

import io.github.quickmsg.edge.mqtt.config.config.SslConfig;

import java.util.List;
import java.util.logging.Level;

/**
 * @author luxurong
 */
public record MqttConfig(List<MqttItem> mqtt,SystemItem system,LogItem log) {
    public static MqttConfig defaultConfig() {
        return new MqttConfig(
                List.of(new MqttItem("0.0.0.0",1883,65535,false,null,false)),
                        new SystemItem(),new LogItem("",false));
    }


    public record SystemItem(){}

    public record MqttItem(String host, int port,int maxMessageSize,
                           boolean wiretap,
                           SslConfig sslConfig,boolean useWebsocket){}


    public record LogItem(String level,boolean persisted){}
}
