package io.github.quickmsg.edge.mqtt;


/**
 * @author luxurong
 */
public class NodeStarter {

    public static void main(String[] args) {
        new MqttContext().start()
                .subscribe();
    }
}