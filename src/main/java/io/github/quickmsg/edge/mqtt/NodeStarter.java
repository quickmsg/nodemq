package io.github.quickmsg.edge.mqtt;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author luxurong
 */
public class NodeStarter {

    public static void main(String[] args) {
        new MqttContext().start()
                .subscribe();
    }
}