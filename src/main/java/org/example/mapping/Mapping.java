package org.example.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Mapping {

    public static Map<String, String> map = new HashMap<>();

    static {
        map.put("/api-gateway/opportunity-service/api/v1/opportunities/\\d+", "/opportunity-service/api/v1/opportunities/{id}");
    }
}
