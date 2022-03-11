package ru.tsc.crm;

import org.apache.logging.log4j.core.config.Configurator;
import ru.tsc.crm.error.exception.ExceptionFactory;
import ru.tsc.crm.quarkus.common.logging.Log4j2ConfigurationFactory;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/api-gateway")
public class ApplicationApiConfig extends Application {

    @PostConstruct
    void init() {
        new ExceptionFactory("999");
        Configurator.reconfigure(new Log4j2ConfigurationFactory().getConfiguration());
    }

}
