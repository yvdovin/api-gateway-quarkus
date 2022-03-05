package ru.tsc.crm.filter;

import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerResponseContext;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.Provider;

import static java.util.Optional.ofNullable;
import static ru.tsc.crm.constant.ContextPropertyKey.DROP_SESSION;
import static ru.tsc.crm.constant.ContextPropertyKey.SESSION_ID;
import static ru.tsc.crm.quarkus.http.util.HttpUtil.resolveHost;

@Priority(Integer.MIN_VALUE + 100)
@Provider
@Singleton
public class SessionIdFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var suspendableContainerResponseContext = (SuspendableContainerResponseContext) responseContext;
        suspendableContainerResponseContext.suspend();
        ofNullable(requestContext.getProperty(SESSION_ID))
                .map(String.class::cast)
                .map(sessionId -> {
                    var host = resolveHost(requestContext);
                    var dropSession = ofNullable(requestContext.getProperty(DROP_SESSION))
                            .map(Boolean.class::cast)
                            .orElse(false);
                    var maxAge = dropSession ? 0 : -1;
                    return new NewCookie(SESSION_ID, sessionId, "/", host, null, maxAge, true, true);
                })
                .ifPresent(cookie -> responseContext.getHeaders().add(HttpHeaders.SET_COOKIE, cookie));
        suspendableContainerResponseContext.resume();
    }
}
