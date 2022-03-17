package ru.tsc.crm.logic;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.RequiredArgsConstructor;
import ru.tsc.crm.error.ModuleOperationCode;
import ru.tsc.crm.mapping.Mapping;
import ru.tsc.crm.service.redis.RedisClientAdapter;
import ru.tsc.crm.service.rest.ProviderMethodClientAdapter;
import ru.tsc.crm.service.rest.ProxyWebClientAdapter;
import org.jboss.resteasy.spi.HttpRequest;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
public class ApiGatewayProxyOperation {

    private final ProxyWebClientAdapter webClientAdapter;
    private final AuthorizationOperation authorizationOperation;

    public Uni<HttpResponse<Buffer>> doCall(HttpRequest request, byte[] body) {
        ModuleOperationCode.PROXY_OPERATION.init();
        return authorizationOperation.doAuthorization(request).flatMap(unused ->
                webClientAdapter.doProxyCall(request, body)
        );
    }
}
