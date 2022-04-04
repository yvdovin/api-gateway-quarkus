package ru.tsc.crm.controller;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jboss.resteasy.spi.HttpRequest;
import ru.tsc.crm.logic.BaseOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static ru.tsc.crm.quarkus.http.constant.HttpHeader.X_B3_SPAN_ID;
import static ru.tsc.crm.quarkus.http.constant.HttpHeader.X_B3_TRACE_ID;

@RequiredArgsConstructor
@Path("")
@Log4j2
public class ApiGatewayController {

    private final BaseOperation baseOperation;

    @GET
    @Path("{var:.+}")
    public Uni<Response> get(@Context HttpRequest request, byte[] body) {
        return baseOperation.doCall(request, body)
                .map(response -> {
                    Response.ResponseBuilder entity = Response.status(response.statusCode())
                            .entity(response.bodyAsJsonObject());
                    copyHeaders(response, entity);
                    return entity.build();
                });
    }

    @POST
    @Path("{var:.+}")
    @Consumes(MediaType.WILDCARD)
    public Uni<Response> post(@Context HttpRequest request, byte[] body) {
        return baseOperation.doCall(request, body)
                .map(response -> {
                    Response.ResponseBuilder entity = Response.status(response.statusCode())
                            .entity(response.bodyAsJsonObject());
                    copyHeaders(response, entity);
                    return entity.build();
                });
    }

    @PUT
    @Path("{var:.+}")
    public Uni<Response> put(@Context HttpRequest request, byte[] body) {
        return baseOperation.doCall(request, body)
                .map(response -> {
                    Response.ResponseBuilder entity = Response.status(response.statusCode())
                            .entity(response.bodyAsJsonObject());
                    copyHeaders(response, entity);
                    return entity.build();
                });
    }

    @DELETE
    @Path("{var:.+}")
    public Uni<Response> delete(@Context HttpRequest request, byte[] body) {
        return baseOperation.doCall(request, body)
                .map(response -> {
                    Response.ResponseBuilder entity = Response.status(response.statusCode())
                            .entity(response.bodyAsJsonObject());
                    copyHeaders(response, entity);
                    return entity.build();
                });
    }

    private void copyHeaders(HttpResponse<Buffer> response, Response.ResponseBuilder builder) {
        response.headers().forEach(header -> {
            var key = header.getKey();
            if (!(HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(key) ||
                    X_B3_TRACE_ID.equals(key) || X_B3_SPAN_ID.equals(key) ||
                    //TODO потом можно убрать
                    HttpHeaders.SET_COOKIE.equals(key))) {
                builder.header(header.getKey(), header.getValue());
            }
        });
    }
}
