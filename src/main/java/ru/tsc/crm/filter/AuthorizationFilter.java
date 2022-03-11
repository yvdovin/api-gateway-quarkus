package ru.tsc.crm.filter;

import io.smallrye.mutiny.Uni;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.MapMessage;
import org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext;
import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerRequestContext;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import ru.tsc.crm.error.exception.ExceptionFactory;
import ru.tsc.crm.error.exception.SecurityException;
import ru.tsc.crm.mapping.Mapping;
import ru.tsc.crm.service.redis.RedisClientAdapter;
import ru.tsc.crm.service.rest.ProviderMethodClientAdapter;
import ru.tsc.crm.session.model.proto.SessionDataOuterClass;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

import static java.util.Optional.of;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static ru.tsc.crm.constant.ContextPropertyKey.DROP_SESSION;
import static ru.tsc.crm.constant.ContextPropertyKey.SESSION_ID;
import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.SecurityExceptionCode.SESSION_ID_IS_ABSENT;
import static ru.tsc.crm.quarkus.http.util.HttpUtil.resolveHost;

@Priority(AUTHORIZATION)
@Provider
@NoArgsConstructor
@Log4j2
@Singleton
public class AuthorizationFilter implements ContainerRequestFilter {

    private RedisClientAdapter redisClientAdapter;
    private ProviderMethodClientAdapter providerMethodClientAdapter;

    private static final String loggingPoint = "AuthorizationFilter.filter";

    @Inject
    public AuthorizationFilter(RedisClientAdapter redisClientAdapter, ProviderMethodClientAdapter providerMethodClientAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.providerMethodClientAdapter = providerMethodClientAdapter;
    }

    @Override
    public void filter(ContainerRequestContext context) {
        var requestContext = (SuspendableContainerRequestContext) context;
        try {
            requestContext.suspend();
            var sessionId = extractSessionId(requestContext);
            resolveCurrentSessionDataBySessionId(sessionId)
                    .flatMap(sessionDataBySessionId -> checkMethods(context, sessionId)
                            .invoke(() -> enrichRequest(context, sessionId)))
                    .onItemOrFailure().invoke((stringSessionDataEntry, e) -> handleResult(requestContext, e))
                    .subscribeAsCompletionStage()
                    .toCompletableFuture();
        } catch (Exception e) {
            handleException(requestContext, e);
        }
    }

    private void handleResult(SuspendableContainerRequestContext requestContext, Throwable throwable) {
        if (throwable != null) {
            handleException(requestContext, throwable);
        } else {
            requestContext.resume();
        }
    }

    private void handleException(SuspendableContainerRequestContext requestContext, Throwable throwable) {
        if (throwable instanceof SecurityException) {
            enrichContextWhenAuthError(requestContext);
        } else if (throwable instanceof CompletionException completionException
                && completionException.getCause() instanceof SecurityException) {
            enrichContextWhenAuthError(requestContext);
        }
        requestContext.resume(throwable);
    }

    private String extractSessionId(ContainerRequestContext context) {
        return extractCookie(context, SESSION_ID)
                .orElseThrow(() -> ExceptionFactory.newSecurityException(resolve(), SESSION_ID_IS_ABSENT, (String) null));
    }

    private Uni<SessionDataOuterClass.SessionData> resolveCurrentSessionDataBySessionId(String sessionId) {
        return redisClientAdapter.refreshSession(sessionId)
                .invoke(() -> {
                    if (log.isDebugEnabled()) {
                        log.debug(new MapMessage<>(Map.of(
                                "point", loggingPoint,
                                "message", "%s сессия обновлена".formatted(sessionId)
                        )));
                    }
                });
    }

    private Uni<Void> checkMethods(ContainerRequestContext context, String sessionId) {
        var method = context.getMethod();
        var joiningMethodWithPath = method + Mapping.map(context.getUriInfo().getPath());
        return providerMethodClientAdapter.checkMethods(joiningMethodWithPath, sessionId, context.getUriInfo().getQueryParameters());
    }

    //TODO пока оставил, но не понятно зачем сдесь добавлять этот хедер
    private void enrichRequest(ContainerRequestContext context, String sessionId) {
//        var preMatchContainerRequestContext = (PreMatchContainerRequestContext) context;
//        var httpHeaders = (ResteasyHttpHeaders) preMatchContainerRequestContext.getHttpRequest().getHttpHeaders();
//        var host = resolveHost(context);
//        var cookie = List.of(
//                new NewCookie(SESSION_ID, sessionId, "/", host, null, -1, true, true).toString()
//        );
//        httpHeaders.getMutableHeaders().put(COOKIE, cookie);
        context.setProperty(SESSION_ID, sessionId);
    }

    /**
     * Нужен для формирования куки в SessionId фильтре
     *
     * @param context
     */
    private void enrichContextWhenAuthError(ContainerRequestContext context) {
        extractCookie(context, SESSION_ID)
                .ifPresent(sessionId -> {
                    context.setProperty(SESSION_ID, sessionId);
                    context.setProperty(DROP_SESSION, true);
                });
    }

    public Optional<String> extractCookie(ContainerRequestContext requestContext, String name) {
        return of(requestContext.getCookies())
                .filter(Predicate.not(Map::isEmpty))
                .map(cookies -> cookies.get(name))
                .map(Cookie::getValue);
    }

}
