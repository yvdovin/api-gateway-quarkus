package org.example.service.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.Cookie;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.function.Consumer;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpHeaders.COOKIE;

@Singleton
public class ProviderMethodClient {
    private final WebClient webClient;
    private final String providerMethodUrl;

    public ProviderMethodClient(@Named("providerMethodWebClient") WebClient webClient,
                                @ConfigProperty(name = "rest.provider.method.url") String providerMethodUrl
    ) {
        this.webClient = webClient;
        this.providerMethodUrl = providerMethodUrl;
    }

    public Uni<String> getMethods() {
        System.out.println("Start web client");
        return webClient.getAbs("%s/spring/getData".formatted(providerMethodUrl))
                //.addQueryParam("type", type)
                //.putHeaders(resolveTracingHeaders())
                //.putHeader(COOKIE.toString(), Cookie.cookie(SESSION_ID, sessionId).encode())
                //.putHeader(CONTENT_TYPE.toString(), ContentType.APPLICATION_JSON.toString())
                .send()
                .map(this::extractResponse)
                .onFailure()
                .invoke(System.out::print);
        //  .invoke(e -> log.error("ProviderMethodClient.getMethods.thrown {}", e.getMessage()));
    }

    private MultiMap resolveTracingHeaders() {
        var headers = MultiMap.caseInsensitiveMultiMap();
        //ofNullable(ThreadContext.get(TRACE_ID)).ifPresent(traceId -> headers.add(X_B3_TRACE_ID, traceId));
        //ofNullable(ThreadContext.get(SPAN_ID)).ifPresent(spanId -> headers.add(X_B3_SPAN_ID, spanId));
        return headers;
    }

    private String extractResponse(HttpResponse<Buffer> httpResponse) {
        int status = httpResponse.statusCode();
        System.out.println(status);
        if (status < 300) {
            String entries = httpResponse.bodyAsString();
            System.out.println(entries);
            return entries;
        }
        throw new RuntimeException();
    }

    public Uni<JsonObject> getMethods(JsonObject request, String sessionId, String type) {
        System.out.println(request);
        return webClient.postAbs("%s/methods/filter".formatted(providerMethodUrl))
                .addQueryParam("type", type)
                .putHeaders(resolveTracingHeaders())
                .putHeader(COOKIE.toString(), Cookie.cookie("sessionId", sessionId).encode())
                .putHeader(CONTENT_TYPE.toString(), "application/json")
                .sendJsonObject(request)
                .map(this::extractResponseJson)
                .onFailure()
                .invoke((Consumer<Throwable>) System.out::println);
    }

    private JsonObject extractResponseJson(HttpResponse<Buffer> httpResponse) {

        int status = httpResponse.statusCode();
        System.out.println(status);
        if (status < 300) {
            return httpResponse.bodyAsJsonObject();
        }
        throw new RuntimeException();
    }
}
