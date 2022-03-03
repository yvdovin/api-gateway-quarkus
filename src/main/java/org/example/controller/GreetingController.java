package org.example.controller;

import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import org.example.mapping.Mapping;
import org.example.service.rest.ProviderMethodClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.regex.Pattern;

@Produces({"application/json;charset=UTF-8"})
@RequiredArgsConstructor
@ApplicationScoped
public class GreetingController {

    private final ProviderMethodClient providerMethodClient;

    @Route(path = "hello/*")
    public Uni<Response> hello(RoutingContext routingContext) {
        System.out.println(routingContext.request().path());
        String s = Mapping.map.keySet()
                .stream()
                .filter(k -> Pattern.matches(k, routingContext.request().path()))
                .findFirst()
                .orElseThrow();
        return providerMethodClient.getMethods()
                .map(item -> Response.accepted(item).status(200).build());
    }
}