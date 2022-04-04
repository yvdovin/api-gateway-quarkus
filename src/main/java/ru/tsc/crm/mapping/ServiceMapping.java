package ru.tsc.crm.mapping;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class ServiceMapping {

    private static final Yaml yaml = new Yaml();
    private static final Map<String, String> serviceMapping;

    static {
        InputStream inputStream = ServiceMapping.class
                .getClassLoader()
                .getResourceAsStream("application.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
        serviceMapping = (Map<String, String>) obj.get("service-mapping");

    }

    public static String getServicePath(String serviceName) {
        return serviceMapping.get(serviceName);
    }
}
