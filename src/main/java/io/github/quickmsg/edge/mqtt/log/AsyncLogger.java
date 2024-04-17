package io.github.quickmsg.edge.mqtt.log;

import io.github.quickmsg.edge.mqtt.config.BootstrapConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

/**
 * @author luxurong
 */
public class AsyncLogger implements Logger {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private java.util.logging.Logger rootLogger;


    public AsyncLogger(BootstrapConfig.LogConfig logConfig) {
        try {
            this.rootLogger = java.util.logging.Logger.getLogger("");
            switch (logConfig.level()) {
                case "info", "INFO" -> this.rootLogger.setLevel(Level.INFO);
                case "debug", "DEBUG" -> this.rootLogger.setLevel(Level.ALL);
                default -> this.rootLogger.setLevel(Level.SEVERE);
            }
              Handler[] handlers = this.rootLogger.getHandlers();
              for (Handler handler : handlers) {
                 this.rootLogger.removeHandler(handler);
            }
             if (logConfig.persisted()) {
                StreamHandler handler = new FileHandler(
                        "logs/NodeMQ-%g.log", 100 * 1024 * 1024, 20, true);
                handler.setFormatter(new CustomFormatter());
                this.rootLogger.addHandler(handler);
            } else {
                 ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setFormatter(new CustomFormatter());
                 this.rootLogger.addHandler(consoleHandler);
            }

        } catch (Exception e) {
            // ignore
         }

     }

    @Override
    public void printInfo(String message) {
        executorService.execute(() -> {
            this.rootLogger.log(Level.INFO, message);
        });
    }

    @Override
    public void printError(String message, Throwable throwable) {
        executorService.execute(() -> {
            this.rootLogger.log(Level.SEVERE, message, throwable);
        });
    }

    public void printWarn(String message) {
        executorService.execute(() -> {
            this.rootLogger.log(Level.WARNING, message);
        });
    }


}
