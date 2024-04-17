package io.github.quickmsg.edge.mqtt;


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