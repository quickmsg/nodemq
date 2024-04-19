package io.github.quickmsg.edge.mqtt.log;

/**
 * @author luxurong
 */
public interface Logger {

    void printInfo(String message);

    void printInfoSync(String message);


    void printError(String message,Throwable throwable);


    void printWarn(String message);

}
