package org.example.logic;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.example.mapping.Mapping;
import org.example.service.redis.RedisClientAdapter;
import org.example.service.rest.ProviderMethodClientAdapter;
import org.example.service.rest.WebClientAdapter;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.regex.Pattern;

@Singleton
@RequiredArgsConstructor
public class ApiGatewayProxyOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final ProviderMethodClientAdapter providerMethodClientAdapter;
    private final WebClientAdapter webClientAdapter;

    //TODO при ошибках с последнего вызова в эксепшн не падать, а пробрасывать
    public Uni<HttpResponse<Buffer>> doCall(RoutingContext routingContext) {
        var sessionId = routingContext.request().getCookie("sessionId").getValue();
        //TODO redis переделать на рефреш
        return redisClientAdapter.getSession(sessionId)
                .flatMap(unused -> {
                            var path = routingContext.request().path();
                            System.out.println(path);
                            var keyForProviderService = Mapping.map.keySet()
                                    .stream()
                                    .filter(k -> Pattern.matches(k, routingContext.request().path()))
                                    .findFirst()
                                    .orElseThrow();
                            var method = routingContext.request().method().toString();
                            var joiningMethodWithPath = method + Mapping.map.get(keyForProviderService);
                            return providerMethodClientAdapter.checkMethods(joiningMethodWithPath, sessionId, null)
                                    .flatMap(u -> webClientAdapter.doCall(routingContext));
                        }
                );
    }
}
