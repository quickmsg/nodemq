package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.endpoint.MqttEndpoint;
import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.github.quickmsg.edge.mqtt.proxy.HAProxyHandler;
import io.github.quickmsg.edge.mqtt.websocket.ByteBufToWebSocketFrameEncoder;
import io.github.quickmsg.edge.mqtt.websocket.WebSocketFrameToByteBufDecoder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpServer;

import java.io.File;
import java.util.UUID;

/**
 * @author luxurong
 */

public class MqttAcceptor implements EndpointAcceptor {

    private final String id;

    private DisposableServer disposableServer;

    public MqttAcceptor() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public Flux<Endpoint<Packet>> accept() {
        return Flux.deferContextual(contextView -> {
            var config = contextView.get(InitConfig.MqttConfig.class);
            var mqttContext = contextView.get(MqttContext.class);
            var tcpServer = config.ssl() != null ? ssl(config.ssl()) : TcpServer.create();
            return Flux.create(contextFluxSink -> {
                tcpServer.port(config.port())
                        .wiretap(false)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .metrics(false)
                        .runOn(LoopResources.create("NodeMQ", Runtime.getRuntime().availableProcessors(), true))
                        .doOnConnection(connection -> {
                            if(config.proxy()){
                                connection.addHandlerLast(new HAProxyMessageDecoder());
                                connection.addHandlerLast(new HAProxyHandler());
                            }
                            if(config.useWebsocket()){
                                connection.addHandlerLast(new HttpServerCodec())
                                        .addHandlerLast(new HttpObjectAggregator(65536))
                                        .addHandlerLast(new WebSocketServerProtocolHandler(config.websocketPath(), "mqtt, mqttv3.1, mqttv3.1.1"))
                                        .addHandlerLast(new WebSocketFrameToByteBufDecoder())
                                        .addHandlerLast(new ByteBufToWebSocketFrameEncoder());
                            }

                            connection
                                    .addHandlerLast(MqttEncoder.INSTANCE)
                                    .addHandlerLast(new MqttDecoder(config.maxMessageSize()));
                            contextFluxSink.next(new MqttEndpoint(mqttContext,config,connection));

                        })
                        .bind()
                        .doOnSuccess(disposableServer -> {
                            this.disposableServer = disposableServer;
                            mqttContext.getLogger().printInfo(
                                    String.format("mqtt start success host：%s port: %d", config.host(), config.port())
                            );
                        })
                        .doOnError(throwable -> {
                            mqttContext.getLogger().printError(
                                    String.format("mqtt start error host：%s port: %d", config.host(), config.port()),throwable
                            );
                        })
                        .subscribe();
            });
        });

    }

    @Override
    public void close() {
        if(disposableServer!=null && !disposableServer.isDisposed()){
            disposableServer.dispose();
        }
    }

    public TcpServer ssl(InitConfig.SslConfig sslConfig) {
        TcpServer server = TcpServer.create();
        server = server.secure(sslContextSpec -> this.secure(sslContextSpec, sslConfig));
        return server;
    }

    private void secure(SslProvider.SslContextSpec sslContextSpec, InitConfig.SslConfig sslConfig) {
        try {
            SslContextBuilder sslContextBuilder;
            if (sslConfig != null) {
                sslContextBuilder = SslContextBuilder.forServer(new File(sslConfig.crt()), new File(sslConfig.key()));
                if (sslConfig.ca() != null) {
                    sslContextBuilder = sslContextBuilder.trustManager(new File(sslConfig.ca()));
                    sslContextBuilder.clientAuth(io.netty.handler.ssl.ClientAuth.REQUIRE);
                }
            } else {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslContextBuilder = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey());
            }
            sslContextSpec.sslContext(sslContextBuilder.build());
        } catch (Exception e) {
//            log.info("ssl error",e);
        }

    }
}
