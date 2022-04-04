package ru.tsc.crm.configuration;

import io.vertx.mutiny.redis.client.Redis;
import ru.tsc.crm.error.exception.ExceptionFactory;
import ru.tsc.crm.quarkus.redis.RedisClient;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import static ru.tsc.crm.error.InvocationExceptionCode.REDIS;
import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.exception.ExceptionFactory.newRetryableException;


@Singleton
public class RedisConfiguration {

    @Singleton
    @Produces
    RedisClient redis(io.vertx.mutiny.redis.client.Redis redis) {
        return new RedisClient(redis, e -> newRetryableException(e, resolve(), REDIS, e.getMessage()));
    }

}
