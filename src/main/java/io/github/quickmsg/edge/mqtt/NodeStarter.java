package io.github.quickmsg.edge.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quickmsg.edge.mqtt.config.MqttConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author luxurong
 */
public class NodeStarter {

    public static void main(String[] args) {
        MqttContext mqttContext = new MqttContext();
        mqttContext.start()
                .subscribe();
    }
}