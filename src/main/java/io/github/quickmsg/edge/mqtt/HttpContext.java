package io.github.quickmsg.edge.mqtt;

import io.github.quickmsg.edge.mqtt.config.InitConfig;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.tcp.SslProvider;

import java.io.File;

/**
 * @author luxurong
 */
public class HttpContext {

    public Mono<DisposableServer> start(InitConfig.HttpConfig config){
        HttpServer httpServer = HttpServer.create();
        if (config.ssl() != null) {
            httpServer.secure(sslContextSpec -> this.loadSsl(sslContextSpec,config.ssl()));
        }
        return httpServer
                .port(config.port())
                .doOnConnection(connection -> connection.addHandler(new JsonObjectDecoder()))
                .route(this::router)
                .accessLog(config.accessLog())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .wiretap(false)
                .bind()
                .cast(DisposableServer.class);
    }

    private void router(HttpServerRoutes httpServerRoutes) {

    }

    private void loadSsl(SslProvider.SslContextSpec sslContextSpec, InitConfig.SslConfig ssl) {
        try {
            SslContextBuilder sslContextBuilder ;
            if (ssl != null) {
                sslContextBuilder = SslContextBuilder.forServer(new File(ssl.crt()), new File(ssl.key()));
                if(ssl.ca()!=null){
                    sslContextBuilder= sslContextBuilder.trustManager(new File(ssl.ca()));
                    sslContextBuilder.clientAuth(io.netty.handler.ssl.ClientAuth.REQUIRE);
                }
            } else {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslContextBuilder = SslContextBuilder.forServer(ssc.certificate(),ssc.privateKey());
            }
            sslContextSpec.sslContext(sslContextBuilder.build());
        } catch (Exception e) {
            // ignore
        }
    }

}
