package ru.tsc.crm.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Mapping {

    public static Map<String, String> map = new ConcurrentHashMap<>();

    static {
        map.put("/api-gateway/opportunity-service/api/v1/opportunities/\\d+", "/opportunity-service/api/v1/opportunities/{id}");
    }

    public static String map(String path) {
        var key = map.keySet()
                .stream()
                .filter(k -> Pattern.matches(k, path))
                .findFirst()
                .orElseThrow();
        return map.get(key);
    }
}
