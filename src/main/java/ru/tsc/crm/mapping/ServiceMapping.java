package ru.tsc.crm.mapping;

import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Map;

@ApplicationScoped
public class ServiceMapping {

    private static final Yaml yaml = new Yaml();
    private Map<String, String> serviceMapping;

    @PostConstruct
    public void init() {
        InputStream inputStream = ServiceMapping.class
                .getClassLoader()
                .getResourceAsStream("application.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
        serviceMapping = (Map<String, String>) obj.get("service-mapping");
    }

    public String getServicePath(String serviceName) {
        return serviceMapping.get(serviceName);
    }
}
