package ru.tsc.crm.service.rest;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.Cookie;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.tsc.crm.error.InvocationExceptionCode;
import ru.tsc.crm.error.SecurityExceptionCode;
import ru.tsc.crm.error.exception.BaseException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.ConnectException;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.vertx.core.http.HttpHeaders.COOKIE;
import static java.util.Optional.ofNullable;
import static ru.tsc.crm.constant.ContextPropertyKey.SESSION_ID;
import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.SecurityExceptionCode.PROVIDER_METHOD_SERVICE;
import static ru.tsc.crm.error.exception.ExceptionFactory.*;
import static ru.tsc.crm.quarkus.common.constant.MdcKey.SPAN_ID;
import static ru.tsc.crm.quarkus.common.constant.MdcKey.TRACE_ID;
import static ru.tsc.crm.quarkus.http.constant.HttpHeader.X_B3_SPAN_ID;
import static ru.tsc.crm.quarkus.http.constant.HttpHeader.X_B3_TRACE_ID;

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

    private MultiMap resolveTracingHeaders() {
        var headers = MultiMap.caseInsensitiveMultiMap();
        ofNullable(ThreadContext.get(TRACE_ID)).ifPresent(traceId -> headers.add(X_B3_TRACE_ID, traceId));
        ofNullable(ThreadContext.get(SPAN_ID)).ifPresent(spanId -> headers.add(X_B3_SPAN_ID, spanId));
        return headers;
    }

    public Uni<JsonObject> getMethods(JsonObject request, String sessionId, String type) {
        return webClient.postAbs("%s/methods/filter".formatted(providerMethodUrl))
                .addQueryParam("type", type)
                .putHeaders(resolveTracingHeaders())
                .putHeader(COOKIE.toString(), Cookie.cookie(SESSION_ID, sessionId).encode())
                .sendJson(request)
                .map(Unchecked.function(response -> {
                    int statusCode = response.statusCode();
                    var responseBody = response.bodyAsString();
                    if (OK.code() == statusCode) {
                        return response.bodyAsJsonObject();
                    }
                    BaseException exception;
                    var operationCode = resolve();
                    if (FORBIDDEN.code() == response.statusCode() || UNAUTHORIZED.code() == response.statusCode()) {
                        exception = newSecurityException(operationCode, SecurityExceptionCode.PROVIDER_METHOD_SERVICE, responseBody);
                    } else {
                        exception = INTERNAL_SERVER_ERROR.code() <= response.statusCode()
                                ? newRetryableException(operationCode, InvocationExceptionCode.PROVIDER_METHOD_SERVICE, responseBody)
                                : newInvocationException(operationCode, InvocationExceptionCode.PROVIDER_METHOD_SERVICE, responseBody);
                    }
                    ofNullable(response.bodyAsJsonObject())
                            .map(errorResponse -> errorResponse.getJsonObject("payload"))
                            .map(payload -> payload.getJsonObject("error"))
                            .ifPresent(error -> exception
                                    .setOriginalCode(error.getString("code"))
                                    .setOriginalMessage(error.getString("message")));
                    //log.error("ProviderMethodServiceClient.getMethods.thrown {}", exception.getMessage());
                    throw exception;
                }))
                .onFailure(ConnectException.class)
                .transform(e -> newRetryableException(e, resolve(), InvocationExceptionCode.PROVIDER_METHOD_SERVICE));
    }
}
