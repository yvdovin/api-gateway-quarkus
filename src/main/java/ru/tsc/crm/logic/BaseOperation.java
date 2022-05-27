package ru.tsc.crm.logic;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.MapMessage;
import org.jboss.resteasy.spi.HttpRequest;
import ru.tsc.crm.error.ModuleOperationCode;
import ru.tsc.crm.error.exception.ExceptionFactory;
import ru.tsc.crm.mapping.UrlMapping;
import ru.tsc.crm.service.redis.RedisClientAdapter;
import ru.tsc.crm.service.rest.ProviderMethodClientAdapter;
import ru.tsc.crm.service.rest.ProxyWebClientAdapter;
import ru.tsc.crm.session.model.proto.SessionDataOuterClass;

import javax.inject.Singleton;
import java.util.Map;

import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.SecurityExceptionCode.SESSION_ID_IS_ABSENT;
import static ru.tsc.crm.util.http.HttpUtils.*;

@Singleton
@RequiredArgsConstructor
@Log4j2
public class BaseOperation {

    private static final String loggingPoint = "BaseOperation";

    private final RedisClientAdapter redisClientAdapter;
    private final ProxyWebClientAdapter proxyWebClientAdapter;
    private final ProviderMethodClientAdapter providerMethodClientAdapter;

    public Uni<HttpResponse<Buffer>> doCall(HttpRequest httpRequest, byte[] body) {
//        ModuleOperationCode.BASE_OPERATION.init();
//        var sessionId = extractCookie(httpRequest, SESSION_ID)
//                .orElseThrow(() -> ExceptionFactory.newSecurityException(resolve(), SESSION_ID_IS_ABSENT, (String) null));
//        var host = resolveHost(httpRequest);
//        return resolveCurrentSessionDataBySessionId(sessionId, host)
//                .flatMap(sessionDataBySessionId -> checkMethods(httpRequest, sessionId)
//                        .flatMap(unused ->
//                                proxyWebClientAdapter.doProxyCall(httpRequest, body)
//                        ));
        return proxyWebClientAdapter.doProxyCall(httpRequest, body);
    }

    private Uni<SessionDataOuterClass.SessionData> resolveCurrentSessionDataBySessionId(String sessionId, String host) {
        return redisClientAdapter.refreshSession(sessionId, host)
                .invoke(() -> {
                    if (log.isDebugEnabled()) {
                        log.debug(new MapMessage<>(Map.of(
                                "point", loggingPoint,
                                "message", "%s сессия обновлена".formatted(sessionId)
                        )));
                    }
                });
    }

    private Uni<Void> checkMethods(HttpRequest httpRequest, String sessionId) {
        var method = httpRequest.getHttpMethod();
        var joiningMethodWithPath = method + UrlMapping.map(httpRequest.getUri().getPath());
        return providerMethodClientAdapter.checkMethods(joiningMethodWithPath, sessionId, httpRequest.getUri().getQueryParameters());
    }

}
