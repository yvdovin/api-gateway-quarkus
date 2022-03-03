package org.example.service.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ProxyWebClient {

    private final WebClient webClient;

    public ProxyWebClient(@Named("proxyWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Uni<HttpResponse<Buffer>> doProxyCall(RoutingContext routingContext) {
        var absoluteURI = routingContext.request().absoluteURI()
                .replace("api-gateway/", "")
                .replace("8081", "8084");
        MultiMap multiMap = new MultiMap(routingContext.request().headers());
        HttpRequest<Buffer> httpRequest = webClient.requestAbs(routingContext.request().method(), absoluteURI)
                .putHeaders(multiMap);
        if (routingContext.getBody() != null) {
            return httpRequest.sendBuffer(Buffer.newInstance(routingContext.getBody()));
        }
        return httpRequest.send();
    }
}
