package ru.tsc.crm.configuration;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.mutiny.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.tsc.crm.service.rest.ProviderMethodClient;
import ru.tsc.crm.service.rest.ProxyWebClient;
import ru.tsc.crm.quarkus.http.client.WebClientInterceptor;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import java.util.concurrent.TimeUnit;

@Singleton
@RequiredArgsConstructor
public class WebClientConfiguration {

    private final Vertx vertx;

    @Singleton
    @Named("providerMethodWebClient")
    @Produces
    WebClient providerMethodWebClient(
            @ConfigProperty(name = "rest.provider.method.connect-timeout", defaultValue = "1000") int connectTimeout,
            @ConfigProperty(name = "rest.provider.method.idle-timeout", defaultValue = "5000") int idleTimeout
    ) {
        var options = new WebClientOptions()
                .setConnectTimeout(connectTimeout)
                .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
                .setIdleTimeout(idleTimeout)
                .setVerifyHost(false)
                .addEnabledSecureTransportProtocol("TLSv1.1")
                .addEnabledSecureTransportProtocol("TLSv1.2");
        var webClientInternal = (WebClientInternal) io.vertx.ext.web.client.WebClient.create(vertx, options);
        webClientInternal.addInterceptor(new WebClientInterceptor(ProviderMethodClient.class));
        return new WebClient(webClientInternal);
    }

    @Singleton
    @Named("proxyWebClient")
    @Produces
    WebClient proxyWebClient(
            @ConfigProperty(name = "rest.proxy.connect-timeout", defaultValue = "1000") int connectTimeout,
            @ConfigProperty(name = "rest.proxy.idle-timeout", defaultValue = "5000") int idleTimeout
    ) {
        var options = new WebClientOptions()
                .setConnectTimeout(connectTimeout)
                .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
                .setIdleTimeout(idleTimeout)
                .setVerifyHost(false)
                .addEnabledSecureTransportProtocol("TLSv1.1")
                .addEnabledSecureTransportProtocol("TLSv1.2");
        var webClientInternal = (WebClientInternal) io.vertx.ext.web.client.WebClient.create(vertx, options);
        webClientInternal.addInterceptor(new WebClientInterceptor(ProxyWebClient.class));
        return new WebClient(webClientInternal);
    }
}
