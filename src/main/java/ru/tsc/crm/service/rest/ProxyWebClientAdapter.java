package ru.tsc.crm.service.rest;

import io.smallrye.mutiny.Uni;

import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.spi.HttpRequest;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
public class ProxyWebClientAdapter {

    private final ProxyWebClient proxyWebClient;

    public Uni<HttpResponse<Buffer>> doProxyCall(RoutingContext routingContext) {
        return proxyWebClient.doProxyCall(routingContext);
    }

    public Uni<HttpResponse<Buffer>> doProxyCall(HttpRequest request, byte[] body) {
        return proxyWebClient.doProxyCall(request, body);
    }
}
