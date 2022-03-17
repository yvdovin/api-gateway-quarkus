package ru.tsc.crm.util.http;

import org.jboss.resteasy.spi.HttpRequest;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Optional.of;


public class HttpUtils {

    public static final String SESSION_ID = "sessionId";

    public static NewCookie createDropSessionIdCookie(HttpRequest httpRequest) {
        var sessionId = extractCookie(httpRequest, SESSION_ID).get();
        var host = resolveHost(httpRequest);
        return new NewCookie(SESSION_ID, sessionId, "/", host, null, 0, true, true);
    }

    public static NewCookie createDropSessionIdCookie(List<String> details) {
        var sessionId = details.get(0);
        var host = details.get(1);
        return new NewCookie(SESSION_ID, sessionId, "/", host, null, 0, true, true);
    }

    public static String resolveHost(HttpRequest httpRequest) {
        return Optional.ofNullable(httpRequest.getHttpHeaders().getRequestHeaders().getFirst("Host")).map((hostWithPort) -> {
            String host = hostWithPort.split(":")[0];
            return "localhost".equals(host) ? "false" : host;
        }).orElseGet(() -> Optional.ofNullable(httpRequest.getUri()).map(UriInfo::getBaseUri).map(URI::getHost).orElse(null));
    }

    public static Optional<String> extractCookie(HttpRequest httpRequest, String name) {
        return of(httpRequest.getHttpHeaders().getCookies())
                .filter(Predicate.not(Map::isEmpty))
                .map(cookies -> cookies.get(name))
                .map(Cookie::getValue);
    }

}
