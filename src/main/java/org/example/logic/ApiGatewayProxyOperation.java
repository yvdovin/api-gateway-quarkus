package org.example.logic;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.example.mapping.Dispatcher;
import org.example.service.redis.RedisClientAdapter;
import org.example.service.rest.ProviderMethodClientAdapter;
import org.example.service.rest.ProxyWebClientAdapter;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
public class ApiGatewayProxyOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final ProviderMethodClientAdapter providerMethodClientAdapter;
    private final ProxyWebClientAdapter webClientAdapter;

    //TODO при ошибках с последнего вызова в эксепшн не падать, а пробрасывать
    public Uni<HttpResponse<Buffer>> doCall(RoutingContext routingContext) {
        var sessionId = routingContext.request().getCookie("sessionId").getValue();
        return redisClientAdapter.refreshSession(sessionId)
                .flatMap(unused -> {
                            var method = routingContext.request().method().toString();
                            var joiningMethodWithPath = method + Dispatcher.dispatch(routingContext.request().path());
                            return providerMethodClientAdapter.checkMethods(joiningMethodWithPath, sessionId, null)
                                    .flatMap(u -> webClientAdapter.doProxyCall(routingContext));
                        }
                );
    }
}
