package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.auth.MqttAuthenticator;

import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpointRegistry;
import io.github.quickmsg.edge.mqtt.loadbalance.HashLoadBalancer;
import io.github.quickmsg.edge.mqtt.loadbalance.LoadBalancer;
import io.github.quickmsg.edge.mqtt.loadbalance.RandomLoadBalancer;
import io.github.quickmsg.edge.mqtt.log.AsyncLogger;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.process.MqttProcessor;
import io.github.quickmsg.edge.mqtt.retry.RetryManager;
import io.github.quickmsg.edge.mqtt.retry.RetryMessage;
import io.github.quickmsg.edge.mqtt.retry.RetryTask;
import io.github.quickmsg.edge.mqtt.retry.TimeAckManager;
import io.github.quickmsg.edge.mqtt.topic.MqttTopicRegistry;
import io.github.quickmsg.edge.mqtt.topic.SubscribeTopic;
import io.github.quickmsg.edge.mqtt.util.JsonReader;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author luxurong
 */
public class MqttContext implements Context, Consumer<Packet> {

    private final Map<String, MqttAcceptor> mqttContext = new HashMap<>();

    private final EndpointRegistry endpointRegistry;

    private final TopicRegistry topicRegistry;

    private final MqttProcessor mqttProcessor;

    private final Authenticator authenticator;


    private RetryManager<RetryMessage, Packet> retryManager;

    private AsyncLogger asyncLogger;

    private InitConfig mqttConfig;

    private final Scheduler scheduler;

    private final RetainStore retainStore;


    private LoadBalancer<SubscribeTopic> loadBalancer;


    public MqttContext() {
        this(new MqttEndpointRegistry(), new MqttTopicRegistry(), new MqttAuthenticator());
    }

    public MqttContext(EndpointRegistry endpointRegistry, TopicRegistry topicRegistry, Authenticator authenticator) {
        this.scheduler = Schedulers.newParallel("event", Runtime.getRuntime().availableProcessors());
        this.endpointRegistry = endpointRegistry;
        this.topicRegistry = topicRegistry;
        this.mqttProcessor = new MqttProcessor(this);
        this.authenticator = authenticator;
        this.retainStore = new RetainStore();
    }



    @Override
    public Flux<Packet> start() {
        this.mqttConfig = readConfig();
        if (this.mqttConfig == null) {
            this.mqttConfig = InitConfig.defaultConfig();
            this.generateJson(mqttConfig);
        }
        this.loadBalancer = switch (mqttConfig.system().shareStrategy()) {
            case HASH -> new HashLoadBalancer<>();
            case RANDOM -> new RandomLoadBalancer<>();
        };
        this.retryManager = new TimeAckManager<>(1000, TimeUnit.SECONDS, 2048,
                mqttConfig.system().unConfirmFlightWindowSize(),
                this::doPacketRetry);
        this.asyncLogger = new AsyncLogger(this.mqttConfig.log());
        this.printBanner(this.asyncLogger);
//        this.asyncLogger.printInfo(JsonReader.bean2Json(mqttConfig));
        return Flux.fromIterable(mqttConfig.mqtt())
                .flatMap(mqttItem -> {
                    var mqttAcceptor = new MqttAcceptor();
                    mqttContext.put(mqttAcceptor.id(), mqttAcceptor);
                    return mqttAcceptor.accept()
                            .contextWrite(context -> context.put(InitConfig.MqttConfig.class, mqttItem))
                            .contextWrite(context -> context.put(MqttContext.class, this));
                })
                .flatMap(Endpoint::receive)
                .subscribeOn(Schedulers.newParallel("event", Runtime.getRuntime().availableProcessors()))
                .doOnNext(this)
                .onErrorContinue((throwable, o) -> {
                    this.asyncLogger.printError("mqtt accept error", throwable);
                });
    }

    private void generateJson(InitConfig mqttConfig) {
        // 使用 NIO 写入文件
        Path path = Paths.get("mqtt.json");
        try {
            // 使用 Files.write() 方法写入文件，如果文件不存在则会创建，如果文件已存在则会覆盖
            Files.writeString(path, JsonReader.bean2PrettyJson(mqttConfig)
                    , StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }catch (Exception e){
            // ignore
        }
    }

    private void printBanner(AsyncLogger asyncLogger) {
        try (InputStream is = NodeStarter.class.getResourceAsStream("/banner.txt")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                asyncLogger.printInfoSync(line);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public Map<String, MqttAcceptor> getMqttAcceptors() {
        return this.mqttContext;
    }

    @Override
    public TopicRegistry getTopicRegistry() {
        return this.topicRegistry;
    }

    @Override
    public EndpointRegistry getChannelRegistry() {
        return this.endpointRegistry;
    }

    @Override
    public Authenticator getAuthenticator() {
        return this.authenticator;
    }

    @Override
    public InitConfig getMqttConfig() {
        return mqttConfig;
    }

    @Override
    public AsyncLogger getLogger() {
        return this.asyncLogger;
    }

    @Override
    public LoadBalancer<SubscribeTopic> getLoadBalancer() {
        return this.loadBalancer;
    }

    private InitConfig readConfig() {
        return JsonReader.readJson("mqtt.json", InitConfig.class);
    }

    @Override
    public RetainStore getRetainStore() {
        return retainStore;
    }

    @Override
    public void accept(Packet packet) {
        switch (packet) {
            case PublishPacket publishPacket -> {
                if (publishPacket.endpoint().connected()) {
                    mqttProcessor.processPublish(publishPacket)
                            .subscribeOn(scheduler).subscribe();
                }
            }
            case SubscribePacket subscribePacket -> {
                if (subscribePacket.endpoint().connected()) {
                    mqttProcessor.processSubscribe(subscribePacket)
                            .subscribeOn(scheduler).subscribe();

                }
            }
            case ConnectPacket connectPacket -> mqttProcessor.processConnect(connectPacket)
                    .subscribeOn(scheduler).subscribe();
            case DisconnectPacket disconnectPacket -> mqttProcessor.processDisconnect(disconnectPacket)
                    .subscribeOn(scheduler).subscribe();
            case PublishAckPacket publishAckPacket -> {
                if (publishAckPacket.endpoint().connected()) {
                    mqttProcessor.processPublishAck(publishAckPacket)
                            .subscribeOn(scheduler).subscribe();
                }
            }
            case AuthPacket authPacket -> {
                if (authPacket.endpoint().connected()) {
                    mqttProcessor.processAuth(authPacket)
                            .subscribeOn(scheduler).subscribe();
                }
            }
            case UnsubscribePacket unsubscribePacket -> {
                if (unsubscribePacket.endpoint().connected()) {
                    mqttProcessor.processUnSubscribe(unsubscribePacket)
                            .subscribeOn(scheduler).subscribe();
                }
            }
            case PublishRelPacket publishRelPacket -> {
                if (publishRelPacket.endpoint().connected()) {
                    mqttProcessor.processPublishRel(publishRelPacket)
                            .subscribeOn(scheduler).subscribe();
                }
            }
            case PublishRecPacket publishRecPacket -> {
                if (publishRecPacket.endpoint().connected()) {
                    mqttProcessor.processPublishRec(publishRecPacket)
                            .subscribeOn(scheduler).subscribe();
                }
            }
            case PublishCompPacket publishCompPacket -> {
                if (publishCompPacket.endpoint().connected()) {
                    mqttProcessor.processPublishComp(publishCompPacket)
                            .subscribeOn(scheduler).subscribe();
                }
            }
            case PingPacket pingPacket -> {
                if (pingPacket.endpoint().connected()) {
                    mqttProcessor.processPing(pingPacket)
                            .subscribeOn(scheduler).subscribe();
                }
            }
            case ClosePacket closePacket -> mqttProcessor.processClose(closePacket)
                    .subscribeOn(scheduler).subscribe();
            default -> {
            }
        }
    }

    private void doPacketRetry(RetryTask<RetryMessage, Packet> retryTask) {
        var endpoint = this.getChannelRegistry().getEndpoint(retryTask.getK().clientId());
        if (endpoint == null || endpoint.isClosed()) {
            retryTask.cancel();
            return;
        }
        switch (retryTask.getM()) {
            case PublishPacket publishPacket -> {
                endpoint.writeMessage(publishPacket,false);
            }
            case PublishRecPacket publishRecPacket -> endpoint.writePublishRec(publishRecPacket,false);
            case PublishRelPacket publishRelPacket -> endpoint.writePublishRel(publishRelPacket,false);
            case null, default -> {
            }
        }
    }

    public RetryManager<RetryMessage, Packet> getRetryManager() {
        return retryManager;
    }
}
