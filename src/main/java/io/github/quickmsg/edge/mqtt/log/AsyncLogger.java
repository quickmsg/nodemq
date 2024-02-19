package io.github.quickmsg.edge.mqtt.log;

import io.github.quickmsg.edge.mqtt.config.MqttConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

/**
 * @author luxurong
 */
public class AsyncLogger implements Logger {

    private  final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private  java.util.logging.Logger rootLogger ;


    public AsyncLogger(MqttConfig.LogItem logItem) {
        try {
            this.rootLogger = java.util.logging.Logger.getLogger("");
            Handler[] handlers = this.rootLogger.getHandlers();
            for (Handler handler : handlers) {
                this.rootLogger.removeHandler(handler);
            }
            if(logItem.persisted()){
                StreamHandler  handler = new FileHandler(
                        "logs/NodeMQ-%g.log", 100*1024 * 1024, 20, true);
                handler.setFormatter(new CustomFormatter());
                this.rootLogger.addHandler(handler);
            }
            else{
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setFormatter(new CustomFormatter());
                this.rootLogger.addHandler(consoleHandler);
            }

        }catch (Exception e){
            // ignore
        }

    }

    @Override
    public void printInfo(String message) {
        executorService.execute(()->{
            this.rootLogger.log(Level.INFO,message);
        });
    }

    @Override
    public void printError(String message, Throwable throwable) {
        executorService.execute(()->{
            this.rootLogger.log(Level.SEVERE,message);
        });
    }

    public void printWarn(String message){
        executorService.execute(()->{
            this.rootLogger.log(Level.SEVERE,message);
        });
    }


}
