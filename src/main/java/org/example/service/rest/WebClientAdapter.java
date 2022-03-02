package org.example.service.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
@RequiredArgsConstructor
public class WebClientAdapter {

    private final Vertx vertx;

    public Uni<HttpResponse<Buffer>> doCall(RoutingContext routingContext) {
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
        WebClient webClient = new WebClient(webClientInternal);
        var absoluteURI = routingContext.request().absoluteURI()
                .replace("api-gateway/", "")
                .replace("8081", "8084");
        System.out.println(absoluteURI);
        MultiMap multiMap = new MultiMap(routingContext.request().headers());
        //Todo body
        return webClient.requestAbs(routingContext.request().method(), absoluteURI)
                .putHeaders(multiMap)
                .send();
    }
}
