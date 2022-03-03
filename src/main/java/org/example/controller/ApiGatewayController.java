package org.example.controller;

import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import lombok.RequiredArgsConstructor;
import org.example.logic.ApiGatewayProxyOperation;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Produces;

//@Produces({"application/json;charset=UTF-8"})
@RequiredArgsConstructor
@ApplicationScoped
public class ApiGatewayController {

    private final ApiGatewayProxyOperation apiGatewayProxyOperation;

    @Route(path = "api-gateway/*")
    public Uni<JsonObject> doCall(RoutingContext routingContext) {
        return apiGatewayProxyOperation.doCall(routingContext)
                .map(response -> {
                    copyHeaders(response, routingContext);
                    routingContext.response().setStatusCode(response.statusCode());
                    return response.bodyAsJsonObject();
                })
                .onFailure()
                .invoke(() -> System.out.println("FUCKING ERROR"));
    }

    private void copyHeaders(HttpResponse<Buffer> response, RoutingContext routingContext) {
        response.headers().forEach(header -> {
            if (!"content-length".equals(header.getKey()))
                routingContext.response().putHeader(header.getKey(), header.getValue());
        });
    }
}
