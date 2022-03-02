package org.example.configuration;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import java.util.concurrent.TimeUnit;

@Singleton
@RequiredArgsConstructor
public class WebClientConfiguration {

    private final Vertx vertx;

    @Singleton
    @Named("providerMethodWebClient")
    @Produces
    WebClient providerMethodWebClient(
            //@ConfigProperty(name = "rest.provider.method.connect-timeout", defaultValue = "1000") int connectTimeout,
            //@ConfigProperty(name = "rest.provider.method.idle-timeout", defaultValue = "5000") int idleTimeout
    ) {
        var options = new WebClientOptions()
                .setConnectTimeout(1000)
                .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
                .setIdleTimeout(5000)
                .setVerifyHost(false)
                .addEnabledSecureTransportProtocol("TLSv1.1")
                .addEnabledSecureTransportProtocol("TLSv1.2");
        //var vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));
        var webClientInternal = (WebClientInternal) io.vertx.ext.web.client.WebClient.create(vertx, options);
        //webClientInternal.addInterceptor(new WebClientInterceptor(ProviderMethodClient.class));
        return new WebClient(webClientInternal);
    }
}
