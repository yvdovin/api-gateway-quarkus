package ru.tsc.crm.service.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.SecurityExceptionCode.METHOD_ACCESS_DENIED;
import static ru.tsc.crm.error.SecurityExceptionCode.SUB_METHOD_ACCESS_DENIED;
import static ru.tsc.crm.error.exception.ExceptionFactory.newSecurityException;

@Singleton
public class ProviderMethodClientAdapter {

    private final ProviderMethodClient client;
    private final String filterRequestType;

    public ProviderMethodClientAdapter(ProviderMethodClient client,
                                       @ConfigProperty(name = "rest.provider-method-service.filter.query-params.type", defaultValue = "session") String filterRequestType) {
        this.client = client;
        this.filterRequestType = filterRequestType;
    }

    public Uni<Void> checkMethods(String method, String sessionId, MultivaluedMap<String, String> queryParams) {
        var queryPostMethod = new JsonObject().put("methods", new JsonArray().add(method));
        return client.getMethods(queryPostMethod, sessionId, filterRequestType)
                .invoke(jsonObject -> ofNullable(jsonObject.getJsonObject("payload"))
                        .map(payload -> payload.getJsonObject("methods"))
                        .map(methods -> methods.getJsonObject(method))
                        .ifPresentOrElse(methodShort -> {
                            var subMethods = methodShort.getJsonArray("subMethods");
                            var submethodOptional = subMethods.getList().stream()
                                    .filter(Objects::nonNull)
                                    .findFirst();
                            if (submethodOptional.isEmpty()) {
                                return;
                            }
                            subMethodLoop:
                            for (var subMethod : subMethods) {
                                var jsonObjectSubMethod = (JsonObject) subMethod;
                                var queryParamValues = queryParams.get(jsonObjectSubMethod.getString("parameterName"));
                                if (queryParamValues != null && !queryParamValues.isEmpty()) {
                                    for (var queryParamValue : queryParamValues) {
                                        if (jsonObjectSubMethod.getJsonArray("parameterValues").contains(queryParamValue)) {
                                            continue subMethodLoop;
                                        }
                                    }
                                }
                                throw newSecurityException(resolve(), SUB_METHOD_ACCESS_DENIED, "Метод='%s', подметод='%s'".formatted(method, subMethod));
                            }

                        }, () -> {
                            throw newSecurityException(resolve(), METHOD_ACCESS_DENIED, "Метод='%s'".formatted(method));
                        })
                )
                .map(u -> null);
    }
}
