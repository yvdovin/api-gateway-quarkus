package org.example.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Dispatcher {

    public static Map<String, String> map = new HashMap<>();

    static {
        map.put("/api-gateway/opportunity-service/api/v1/opportunities/\\d+", "/opportunity-service/api/v1/opportunities/{id}");
    }

    public static String dispatch(String path) {
        var key = map.keySet()
                .stream()
                .filter(k -> Pattern.matches(k, path))
                .findFirst()
                .orElseThrow();
        return map.get(key);
    }
}
