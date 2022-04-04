package ru.tsc.crm.service.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import ru.tsc.crm.mapping.ServiceMapping;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ProxyWebClient {

    private final WebClient webClient;
    private final ServiceMapping serviceMapping;

    public ProxyWebClient(@Named("proxyWebClient") WebClient webClient, ServiceMapping serviceMapping) {
        this.webClient = webClient;
        this.serviceMapping = serviceMapping;
    }

    public Uni<HttpResponse<Buffer>> doProxyCall(org.jboss.resteasy.spi.HttpRequest request, byte[] body) {
        var absoluteURI = createAbsoluteUri(request.getUri().getRequestUri().toString());
        HttpRequest<Buffer> bufferHttpRequest = webClient.requestAbs(HttpMethod.valueOf(request.getHttpMethod()), absoluteURI);
        copyHeaders(request, bufferHttpRequest);
        if (request.getInputStream() != null) {
            return bufferHttpRequest.sendBuffer(Buffer.buffer(body));
        }
        return bufferHttpRequest.send();
    }


    /**
     * Удаляем хедер Host для локального тестирования
     */
    private void copyHeaders(org.jboss.resteasy.spi.HttpRequest request, HttpRequest<Buffer> bufferHttpRequest) {
        request.getHttpHeaders().getRequestHeaders().forEach(bufferHttpRequest::putHeader);
        if (bufferHttpRequest.headers().get("Host").contains("localhost")) {
            bufferHttpRequest.headers().remove("Host");
        }
    }

    private String extractServiceName(String uri) {
        var tmp = uri.substring(uri.indexOf("api-gateway/") + "api-gateway/".length());
        return tmp.substring(0, tmp.indexOf('/'));
    }

    private String createAbsoluteUri(String uri) {
        var serviceName = extractServiceName(uri);
        return serviceMapping.getServicePath(serviceName) +
                uri.substring(uri.indexOf(serviceName) + serviceName.length());
    }
}
