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
            // 获取根日志记录器
            this.rootLogger = java.util.logging.Logger.getLogger(this.getClass().getName());

            // 移除默认的处理器
            Handler[] handlers = this.rootLogger.getHandlers();
            for (Handler handler : handlers) {
                this.rootLogger.removeHandler(handler);
            }
            StreamHandler handler;
            if(logItem.persisted()){
                // 添加日期切割的文件处理器
                handler = new FileHandler(
                        "logs/NodeMQ-%g.log", 100*1024 * 1024, 20, true);
            }else{
                handler = new ConsoleHandler();
            }
            handler.setFormatter(new SimpleFormatter());
            this.rootLogger.addHandler(handler);
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
