package org.example.controller;

import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import org.example.logic.ApiGatewayProxyOperation;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Produces;

@Produces({"application/json;charset=UTF-8"})
@RequiredArgsConstructor
@ApplicationScoped
public class ApiGatewayController {

    private final ApiGatewayProxyOperation apiGatewayProxyOperation;

    @Route(path = "api-gateway/*")
    public Uni<JsonObject> hello(RoutingContext routingContext) {
        //JsonObject object = new JsonObject("{\"status\":\"Ok\",\"payload\":{\"opportunity\":{\"id\":67296,\"name\":\"Тест1\",\"companyId\":6,\"companyName\":\"ООО Филиал ОСНОВНОЙ\",\"companyShortName\":\"Филиал ОСНОВНОЙ\",\"finalCompanyId\":6,\"finalCompanyName\":\"ООО Филиал ОСНОВНОЙ\",\"finalCompanyShortName\":\"Филиал ОСНОВНОЙ\",\"currency\":{\"dictionaryCode\":\"transaction_currency\",\"code\":\"810\"},\"expectedSignDate\":\"2022-03-03\",\"organizations\":[{\"id\":6,\"main\":true,\"name\":\"Т1 Консалтинг\"}],\"competenceCentre\":{\"dictionaryCode\":\"competences_centers\",\"code\":\"dgc\"},\"opportunityType\":{\"dictionaryCode\":\"opportunity_stage\",\"code\":null},\"salespersonProbability\":0.01,\"legalEntity\":{\"dictionaryCode\":\"legal_entity\",\"code\":\"tvc\"},\"estimatedMarginality\":null,\"expectedRevenue\":100000.0,\"expectedRevenueWithoutVat\":100000.0,\"vatIncluded\":false,\"war\":false,\"requestType\":{\"dictionaryCode\":\"request_type\",\"code\":null},\"submissionDate\":null,\"approved\":false,\"transferred\":false,\"prepareOffer\":false,\"prepareContract\":false,\"status\":{\"dictionaryCode\":\"opportunity_status\",\"code\":\"open\"},\"direction\":{\"dictionaryCode\":\"direction\",\"code\":\"market\"},\"stage\":{\"dictionaryCode\":\"opportunity_stage\",\"code\":\"01\"},\"probabilityForOppStage\":{\"dictionaryCode\":\"probabilities_opportunity_stages\",\"code\":\"01\"},\"createdAt\":\"2022-03-02T15:03:51.428574Z\",\"createdBy\":72,\"updatedAt\":\"2022-03-02T15:03:51.428574Z\",\"updatedBy\":72,\"active\":true,\"opportunityPositionId\":53,\"probabilityBr\":null,\"probability\":0.01,\"initiator\":{\"main\":true,\"id\":53,\"name\":\"Консультант 1\",\"role\":{\"dictionaryCode\":\"positions_types\",\"code\":\"kam\"},\"createdAt\":\"2022-03-02T15:03:51.428574Z\",\"updatedAt\":null,\"user\":{\"id\":72,\"lastName\":\"Ханина\",\"firstName\":\"Ирина\",\"middleName\":\"Владимировна\",\"workPhone\":null,\"workEmail\":\"ikhanina@t1-consulting.ru\"},\"organization\":{\"id\":6,\"name\":\"Т1 Консалтинг\"}}}}}");

        //return Uni.createFrom().item(object);
        return apiGatewayProxyOperation.doCall(routingContext)
                .map(response -> {
                    // TODO Почему это работает, а return в 39 строчке не работает
                    //return new JsonObject();
                    System.out.println("!!!!" + response.statusCode());
                    response.headers().forEach(header -> routingContext
                            .response()
                            .putHeader(header.getKey(), header.getValue()));
                    routingContext.response().setStatusCode(response.statusCode());
                    JsonObject entries = response.bodyAsJsonObject();
                    //Тут все печатает нормально
                    System.out.println(entries);
                    return entries;
                })
                .onFailure()
                .invoke(() -> System.out.println("FUCKING ERROR"));
    }
}
