package org.example.filter;

//import io.smallrye.mutiny.Uni;
//import lombok.NoArgsConstructor;
//import lombok.extern.log4j.Log4j2;
//import org.apache.logging.log4j.ThreadContext;
//import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;
//import org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext;
//import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerRequestContext;
//import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
//import ru.tsc.crm.quarkus.frontendauthorization.annotation.Secured;
//import ru.tsc.crm.quarkus.frontendauthorization.constant.ContextPropertyKey;
//import ru.tsc.crm.quarkus.frontendauthorization.error.exception.FrontendAuthorizationException;
//import ru.tsc.crm.quarkus.frontendauthorization.service.client.redis.RedisClientAdapter;
//import ru.tsc.crm.quarkus.frontendauthorization.service.client.rest.ProviderMethodClientAdapter;
//import ru.tsc.crm.session.model.proto.SessionDataOuterClass;
//
//import javax.annotation.Priority;
//import javax.annotation.security.DenyAll;
//import javax.annotation.security.PermitAll;
//import javax.annotation.security.RolesAllowed;
//import javax.inject.Inject;
//import javax.inject.Singleton;
//import javax.ws.rs.container.ContainerRequestContext;
//import javax.ws.rs.container.ContainerRequestFilter;
//import javax.ws.rs.core.NewCookie;
//import javax.ws.rs.ext.Provider;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.CompletionException;
//
//import static javax.ws.rs.Priorities.AUTHORIZATION;
//import static javax.ws.rs.core.HttpHeaders.COOKIE;
//import static ru.tsc.crm.quarkus.error.message.SecurityExceptionMessage.ACCESS_FOR_USERS_ROLE_IS_DENIED;
//import static ru.tsc.crm.quarkus.error.message.SecurityExceptionMessage.INVALID_COOKIE;
//import static ru.tsc.crm.quarkus.frontendauthorization.constant.ContextPropertyKey.SESSION_ID;
//import static ru.tsc.crm.quarkus.frontendauthorization.error.exception.FrontendAuthorizationException.forbiddenException;
//import static ru.tsc.crm.quarkus.frontendauthorization.error.exception.FrontendAuthorizationException.notAuthorizedException;
//import static ru.tsc.crm.quarkus.frontendauthorization.util.RequestContextParametersExtractor.extractCookie;
//import static ru.tsc.crm.quarkus.http.util.HttpUtil.resolveHost;

//@Priority(AUTHORIZATION)
//@Provider
//@NoArgsConstructor
//@Log4j2
//@Singleton
//public class AuthorizationFilter implements ContainerRequestFilter {

//    private RedisClientAdapter redisClientAdapter;
//    private ProviderMethodClientAdapter providerMethodClientAdapter;
//
//    private static final String loggingPoint = "AuthorizationFilter.filter";
//
//    @Inject
//    public AuthorizationFilter(RedisClientAdapter redisClientAdapter, ProviderMethodClientAdapter providerMethodClientAdapter) {
//        this.redisClientAdapter = redisClientAdapter;
//        this.providerMethodClientAdapter = providerMethodClientAdapter;
//    }
//
//    @Override
//    public void filter(ContainerRequestContext context) {
//        var requestContext = (SuspendableContainerRequestContext) context;
//        try {
//            var method = ((PostMatchContainerRequestContext) context).getResourceMethod().getMethod();
//            requestContext.suspend();
//            if (method.isAnnotationPresent(RolesAllowed.class)) {
//                resolveCurrentSessionDataBySessionId(requestContext)
//                        .invoke(sessionDataBySessionId -> {
//                            ThreadContext.put("user", sessionDataBySessionId.getValue().getUser().getLogin());
//                            var rolesAllowedAnnotation = method.getAnnotation(RolesAllowed.class);
//                            var rolesAllowed = Set.of(rolesAllowedAnnotation.value());
//                            checkRoles(context, sessionDataBySessionId, rolesAllowed);
//                            enrichRequest(context, sessionDataBySessionId.getKey());
//                        })
//                        .onItemOrFailure().invoke((stringSessionDataEntry, throwable) -> handleResult(requestContext, throwable))
//                        .subscribeAsCompletionStage()
//                        .toCompletableFuture();
//                return;
//            } else if (method.isAnnotationPresent(PermitAll.class) || method.isAnnotationPresent(ru.tsc.crm.quarkus.frontendauthorization.annotation.PermitAll.class)) {
//                resolveCurrentSessionDataBySessionId(requestContext)
//                        .invoke(sessionDataBySessionId -> {
//                            ThreadContext.put("user", sessionDataBySessionId.getValue().getUser().getLogin());
//                            enrichRequest(context, sessionDataBySessionId.getKey());
//                        })
//                        .onItemOrFailure().invoke((stringSessionDataEntry, throwable) -> handleResult(requestContext, throwable))
//                        .subscribeAsCompletionStage()
//                        .toCompletableFuture();
//                return;
//            } else if (method.isAnnotationPresent(Secured.class)) {
//                resolveCurrentSessionDataBySessionId(requestContext)
//                        .flatMap(sessionDataBySessionId -> {
//                            ThreadContext.put("user", sessionDataBySessionId.getValue().getUser().getLogin());
//                            return checkMethods(context, sessionDataBySessionId)
//                                    .invoke(() -> enrichRequest(context, sessionDataBySessionId.getKey()));
//                        })
//                        .onItemOrFailure().invoke((stringSessionDataEntry, throwable) -> handleResult(requestContext, throwable))
//                        .subscribeAsCompletionStage()
//                        .toCompletableFuture();
//                return;
//            } else if (method.isAnnotationPresent(DenyAll.class)) {
//                var methodInfo = context.getMethod() + context.getUriInfo().getPath();
//                throw forbiddenException(ACCESS_FOR_USERS_ROLE_IS_DENIED, String.format("Access to method '%s' is denied for all", methodInfo));
//            } else {
//                extractCookie(context, SESSION_ID).ifPresent(sessionId -> enrichRequest(context, sessionId));
//            }
//            requestContext.resume();
//        } catch (Exception e) {
//            handleException(requestContext, e);
//        }
//    }
//
//    private void handleResult(SuspendableContainerRequestContext requestContext, Throwable throwable) {
//        if (throwable != null) {
//            handleException(requestContext, throwable);
//        } else {
//            requestContext.resume();
//        }
//    }
//
//    private void handleException(SuspendableContainerRequestContext requestContext, Throwable throwable) {
//        if (throwable instanceof FrontendAuthorizationException e) {
//            handleFrontendAuthorizationException(requestContext, e);
//        } else if (throwable instanceof CompletionException completionException
//                && completionException.getCause() instanceof FrontendAuthorizationException e) {
//            handleFrontendAuthorizationException(requestContext, e);
//        } else {
//            log.error("{}.thrown", loggingPoint, throwable);
//            var httpException = new FrontendAuthorizationException(500, throwable);
//            requestContext.resume(httpException);
//        }
//    }
//
//    private void handleFrontendAuthorizationException(SuspendableContainerRequestContext requestContext, FrontendAuthorizationException e) {
//        switch (e.getStatus()) {
//            case 401 -> {
//                enrichContextWhenAuthError(requestContext);
//                log.error("{}.thrown.unauthorized", loggingPoint, e);
//            }
//            case 403 -> {
//                enrichContextWhenAuthError(requestContext);
//                log.error("{}.thrown.forbidden", loggingPoint, e);
//            }
//            default -> log.error("{}.thrown", loggingPoint, e);
//        }
//        requestContext.resume(e);
//    }
//
//    private Uni<Map.Entry<String, SessionDataOuterClass.SessionData>> resolveCurrentSessionDataBySessionId(ContainerRequestContext context) {
//        var sessionId = extractCookie(context, SESSION_ID)
//                .orElseThrow(() -> notAuthorizedException(INVALID_COOKIE));
//        return redisClientAdapter.refreshSession(sessionId)
//                .invoke(() -> {
//                    ThreadContext.put(SESSION_ID, sessionId);
//                    log.debug("{} сессия обновлена", loggingPoint);
//                })
//                .map(sessionData -> Map.entry(sessionId, sessionData));
//    }
//
//
//    private Uni<Void> checkMethods(ContainerRequestContext context, Map.Entry<String, SessionDataOuterClass.SessionData> sessionDataBySessionId) {
//        var method = ((PostMatchContainerRequestContext) context).getResourceMethod().getMethod();
//
//        var classPath = Arrays.stream(method.getDeclaringClass().getAnnotations())
//                .filter(annotation -> annotation.annotationType().equals(javax.ws.rs.Path.class))
//                .map(annotation -> ((javax.ws.rs.Path) annotation).value())
//                .filter(path -> !path.trim().isEmpty())
//                .findFirst()
//                .orElseGet(() -> Arrays.stream(method.getDeclaringClass().getInterfaces())
//                        .filter(clazz -> clazz.getAnnotation(javax.ws.rs.Path.class) != null)
//                        .map(clazz -> clazz.getAnnotation(javax.ws.rs.Path.class).value())
//                        .findFirst()
//                        .orElseGet(() -> method.getDeclaringClass().getAnnotation(javax.ws.rs.Path.class).value()));
//
//        var methodPath = Arrays.stream(method.getDeclaringClass().getInterfaces())
//                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
//                .filter(declaredMethod -> declaredMethod.getName().equals(method.getName()) &&
//                        declaredMethod.getAnnotation(javax.ws.rs.Path.class) != null)
//                .map(declaredMethod -> declaredMethod.getAnnotation(javax.ws.rs.Path.class).value())
//                .findFirst()
//                .orElseGet(() -> Arrays.stream(method.getDeclaringClass().getDeclaredMethods())
//                        .filter(declaredMethod -> declaredMethod.getName().equals(method.getName()) &&
//                                declaredMethod.getAnnotation(javax.ws.rs.Path.class) != null)
//                        .map(declaredMethod -> declaredMethod.getAnnotation(javax.ws.rs.Path.class).value())
//                        .findFirst()
//                        .orElse(""));
//
//        String joinedPath =
//                String.join("",
//                                context.getMethod(),
//                                context.getUriInfo().getBaseUri().getPath(),
//                                classPath,
//                                methodPath)
//                        .replace("//", "/");
//
//        return providerMethodClientAdapter.checkMethods(joinedPath, sessionDataBySessionId.getKey(), context.getUriInfo().getQueryParameters());
//    }
//
//
//    private void checkRoles(ContainerRequestContext context, Map.Entry<String, SessionDataOuterClass.SessionData> sessionDataBySessionId, Set<String> rolesAllowed) {
//        var sessionData = sessionDataBySessionId.getValue();
//        for (var role : rolesAllowed) {
//            if (sessionData.getResponsibilitiesList().contains(role)) {
//                return;
//            }
//        }
//        var methodInfo = context.getMethod() + context.getUriInfo().getPath();
//        throw forbiddenException(ACCESS_FOR_USERS_ROLE_IS_DENIED, String.format("User: '%s', Method: '%s'", sessionData.getUser().getLogin(), methodInfo));
//    }
//
//    private void enrichRequest(ContainerRequestContext context, String sessionId) {
//        var preMatchContainerRequestContext = (PreMatchContainerRequestContext) context;
//        var httpHeaders = (ResteasyHttpHeaders) preMatchContainerRequestContext.getHttpRequest().getHttpHeaders();
//        var host = resolveHost(context);
//        var cookie = List.of(
//                new NewCookie(SESSION_ID, sessionId, "/", host, null, -1, true, true).toString()
//        );
//        httpHeaders.getMutableHeaders().put(COOKIE, cookie);
//        context.setProperty(SESSION_ID, sessionId);
//        var method = ((PostMatchContainerRequestContext) context).getResourceMethod().getMethod();
//        var dropSession = (method.isAnnotationPresent(ru.tsc.crm.quarkus.frontendauthorization.annotation.PermitAll.class)
//                && method.getAnnotation(ru.tsc.crm.quarkus.frontendauthorization.annotation.PermitAll.class).dropSession())
//                || (method.isAnnotationPresent(Secured.class) && method.getAnnotation(Secured.class).dropSession());
//        context.setProperty(ContextPropertyKey.DROP_SESSION, dropSession);
//    }
//
//    private void enrichContextWhenAuthError(ContainerRequestContext context) {
//        extractCookie(context, SESSION_ID)
//                .ifPresent(sessionId -> {
//                    context.setProperty(SESSION_ID, sessionId);
//                    context.setProperty(ContextPropertyKey.DROP_SESSION, true);
//                });
//    }

//}
