package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.auth.MqttAuthenticator;

import io.github.quickmsg.edge.mqtt.config.MqttConfig;
import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpointRegistry;
import io.github.quickmsg.edge.mqtt.log.AsyncLogger;
import io.github.quickmsg.edge.mqtt.packet.*;
import io.github.quickmsg.edge.mqtt.process.MqttProcessor;
import io.github.quickmsg.edge.mqtt.topic.MqttTopicRegistry;
import io.github.quickmsg.edge.mqtt.util.JsonReader;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
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

    private  AsyncLogger asyncLogger;


    public MqttContext() {
        this(new MqttEndpointRegistry(), new MqttTopicRegistry(),new MqttAuthenticator());
    }

    public MqttContext(EndpointRegistry endpointRegistry, TopicRegistry topicRegistry, Authenticator authenticator ) {
        this.endpointRegistry = endpointRegistry;
        this.topicRegistry = topicRegistry;
        this.mqttProcessor = new MqttProcessor(this);
        this.authenticator = authenticator;
    }


    @Override
    public Flux<Packet> start() {
        MqttConfig mqttConfig = readConfig();
        if(mqttConfig==null){
            mqttConfig = MqttConfig.defaultConfig();
        }
        else{
            this.checkConfig(mqttConfig);
        }
        this.asyncLogger = new AsyncLogger(mqttConfig.log());
        return Flux.fromIterable(mqttConfig.mqtt())
                .flatMap(mqttItem -> {
                    final MqttAcceptor mqttAcceptor = new MqttAcceptor();
                    mqttContext.put(mqttAcceptor.id(), mqttAcceptor);
                    return mqttAcceptor.accept()
                            .contextWrite(context -> context.put(MqttConfig.MqttItem.class, mqttItem))
                            .contextWrite(context -> context.put(MqttContext.class, this));
                })
                .flatMap(Endpoint::receive)
                .subscribeOn(Schedulers.newParallel("event", Runtime.getRuntime().availableProcessors()))
                .doOnNext(this)
                .onErrorContinue((throwable, o) -> {
                    this.asyncLogger.printError("mqtt accept error",throwable);
                });
    }

    private void checkConfig(MqttConfig mqttConfig) {

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
    public AsyncLogger getLogger() {
        return this.asyncLogger;
    }

    private MqttConfig readConfig() {
        return JsonReader.readJson("mqtt.json",MqttConfig.class);
    }

    @Override
    public void accept(Packet packet) {
        switch (packet) {
            case PublishPacket publishPacket -> mqttProcessor.processPublish(publishPacket)
                    .subscribe();
            case SubscribePacket subscribePacket -> mqttProcessor.processSubscribe(subscribePacket).subscribe();
            case ConnectPacket connectPacket-> mqttProcessor.processConnect(connectPacket).subscribe();
            case DisconnectPacket disconnectPacket->mqttProcessor.processDisconnect(disconnectPacket).subscribe();
            case PublishAckPacket publishAckPacket->mqttProcessor.processPublishAck(publishAckPacket).subscribe();
            case AuthPacket authPacket->mqttProcessor.processAuth(authPacket).subscribe();
            case UnsubscribePacket unsubscribePacket->mqttProcessor.processUnSubscribe(unsubscribePacket).subscribe();
            case PublishRelPacket publishRelPacket ->mqttProcessor.processPublishRel(publishRelPacket).subscribe();
            case PublishRecPacket publishRecPacket->mqttProcessor.processPublishRec(publishRecPacket).subscribe();
            case PublishCompPacket publishCompPacket->mqttProcessor.processPublishComp(publishCompPacket).subscribe();
            case PingPacket pingPacket->mqttProcessor.processPing(pingPacket).subscribe();
            case ClosePacket closePacket->mqttProcessor.processClose(closePacket).subscribe();
            default -> {
            }
        }
    }
}
