package ru.tsc.crm.mapping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static ru.tsc.crm.error.BusinessExceptionCode.METHOD_NOT_FOUND;
import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.exception.ExceptionFactory.newBusinessException;

public class Mapping {

    public static Map<String, String> map = new ConcurrentHashMap<>();

    static {
        map.put("/opportunity-service/api/v1/opportunities/\\d+", "/opportunity-service/api/v1/opportunities/{id}");
    }

    public static String map(String path) {
        var key = map.keySet()
                .stream()
                .filter(k -> Pattern.matches(k, path))
                .findFirst()
                .orElseThrow(
                        () -> newBusinessException(resolve(), METHOD_NOT_FOUND, (String) null)
                );
        return map.get(key);
    }
}
