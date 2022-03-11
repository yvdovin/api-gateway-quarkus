package ru.tsc.crm.configuration;

import io.vertx.mutiny.redis.client.Redis;
import ru.tsc.crm.error.exception.ExceptionFactory;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;


@Singleton
public class RedisConfiguration {

    /**
     * {@link io.vertx.mutiny.redis.client.Redis} не конфигурируется при наличии пропертей "quarkus.redis"
     *
     * @param redis - конфигурируется автоматически при наличии пропертей "quarkus.redis"
     * @return сконфигурированный {@link io.vertx.mutiny.redis.client.Redis}
     */
    @Singleton
    @Produces
    Redis redis(io.vertx.redis.client.Redis redis) {
        return new Redis(redis);
    }

}
