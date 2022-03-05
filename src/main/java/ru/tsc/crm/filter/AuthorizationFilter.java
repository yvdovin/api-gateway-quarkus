package ru.tsc.crm.filter;

import io.smallrye.mutiny.Uni;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import ru.tsc.crm.error.exception.AuthorizationException;
import ru.tsc.crm.mapping.Mapping;
import ru.tsc.crm.service.redis.RedisClientAdapter;
import ru.tsc.crm.service.rest.ProviderMethodClientAdapter;
import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerRequestContext;
import ru.tsc.crm.session.model.proto.SessionDataOuterClass;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
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
import static ru.tsc.crm.error.exception.AuthorizationException.notAuthorizedException;
import static ru.tsc.crm.quarkus.error.message.SecurityExceptionMessage.INVALID_COOKIE;
import static ru.tsc.crm.quarkus.http.util.HttpUtil.resolveHost;

@Priority(AUTHORIZATION)
//Todo понять как работает эта аннотация и почему работает в утил либе
//@Provider
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
            resolveCurrentSessionDataBySessionId(requestContext)
                    .flatMap(sessionDataBySessionId -> {
                        ThreadContext.put("user", sessionDataBySessionId.getValue().getUser().getLogin());
                        return checkMethods(context, sessionDataBySessionId)
                               .invoke(() -> enrichRequest(context, sessionDataBySessionId.getKey()));
                    })
                    .onItemOrFailure().invoke((stringSessionDataEntry, throwable) -> handleResult(requestContext, throwable))
                    .subscribeAsCompletionStage()
                    .toCompletableFuture();
            requestContext.resume();
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
        if (throwable instanceof AuthorizationException e) {
            handleFrontendAuthorizationException(requestContext, e);
        } else if (throwable instanceof CompletionException completionException
                && completionException.getCause() instanceof AuthorizationException e) {
            handleFrontendAuthorizationException(requestContext, e);
        } else {
            log.error("{}.thrown", loggingPoint, throwable);
            var httpException = new AuthorizationException(500, throwable);
            requestContext.resume(httpException);
        }
    }

    private void handleFrontendAuthorizationException(SuspendableContainerRequestContext requestContext, AuthorizationException e) {
        switch (e.getStatus()) {
            case 401 -> {
                enrichContextWhenAuthError(requestContext);
                log.error("{}.thrown.unauthorized", loggingPoint, e);
            }
            case 403 -> {
                enrichContextWhenAuthError(requestContext);
                log.error("{}.thrown.forbidden", loggingPoint, e);
            }
            default -> log.error("{}.thrown", loggingPoint, e);
        }
        requestContext.resume(e);
    }

    private Uni<Map.Entry<String, SessionDataOuterClass.SessionData>> resolveCurrentSessionDataBySessionId(ContainerRequestContext context) {
        var sessionId = extractCookie(context, SESSION_ID)
                .orElseThrow(() -> notAuthorizedException(INVALID_COOKIE));
        return redisClientAdapter.refreshSession(sessionId)
                .invoke(() -> {
                    ThreadContext.put(SESSION_ID, sessionId);
                    log.debug("{} сессия обновлена", loggingPoint);
                })
                .map(sessionData -> Map.entry(sessionId, sessionData));
    }

    private Uni<Void> checkMethods(ContainerRequestContext context, Map.Entry<String, SessionDataOuterClass.SessionData> sessionDataBySessionId) {
        var method = context.getMethod();
        var joiningMethodWithPath = method + Mapping.map(context.getUriInfo().getPath());
        return providerMethodClientAdapter.checkMethods(joiningMethodWithPath, sessionDataBySessionId.getKey(), context.getUriInfo().getQueryParameters());
    }

    //TODO пока оставил, но не понятно зачем сдесь добавлять этот хедер
    private void enrichRequest(ContainerRequestContext context, String sessionId) {
        var preMatchContainerRequestContext = (PreMatchContainerRequestContext) context;
        var httpHeaders = (ResteasyHttpHeaders) preMatchContainerRequestContext.getHttpRequest().getHttpHeaders();
        var host = resolveHost(context);
        var cookie = List.of(
                new NewCookie(SESSION_ID, sessionId, "/", host, null, -1, true, true).toString()
        );
        httpHeaders.getMutableHeaders().put(COOKIE, cookie);
        context.setProperty(SESSION_ID, sessionId);
    }

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
