package ru.tsc.crm.service.redis;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.message.MapMessage;
import ru.tsc.crm.error.exception.ExceptionFactory;
import ru.tsc.crm.quarkus.common.uuid.UuidUtils;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static ru.tsc.crm.error.InvocationExceptionCode.REDIS;
import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.exception.ExceptionFactory.newRetryableException;

@Singleton
@RequiredArgsConstructor
@Log4j2
public class RedisClient {

    private final Redis redis;

    public <T> Uni<T> get(String key, Function<Response, T> mapper, Function<T, String> logMapper) {
        var redisRequestId = UuidUtils.generateDefaultUuid();
        var startMillis = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            withRedisRequestId(redisRequestId, () -> log.debug(new MapMessage<>(Map.of(
                    "point", "RedisClient.get.in",
                    "redis", Map.of(
                            "key", key
                    )))));
        }
        return redis.send(Request.cmd(Command.GET).arg(key))
                .map(response -> {
                    if (response != null) {
                        var result = mapper.apply(response);
                        if (log.isDebugEnabled()) {
                            if (log.isTraceEnabled()) {
                                var loggedResult = ofNullable(result).map(logMapper).orElse(null);
                                withRedisRequestId(redisRequestId, () -> log.trace(new MapMessage<>(Map.of(
                                        "point", "RedisClient.get.out",
                                        "redis", Map.of(
                                                "result", String.valueOf(loggedResult),
                                                "time", System.currentTimeMillis() - startMillis
                                        )))));
                            } else {
                                withRedisRequestId(redisRequestId, () -> log.debug(new MapMessage<>(Map.of(
                                        "point", "RedisClient.get.out",
                                        "redis", Map.of(
                                                "time", System.currentTimeMillis() - startMillis
                                        )))));
                            }
                        }
                        return result;
                    }
                    if (log.isDebugEnabled()) {
                        withRedisRequestId(redisRequestId, () -> log.debug(new MapMessage<>(Map.of(
                                "point", "RedisClient.get.out",
                                "redis", Map.of(
                                        "result", "null",
                                        "time", System.currentTimeMillis() - startMillis
                                )))));
                    }
                    return null;
                })
                .onFailure()
                .transform(e -> {
                    withRedisRequestId(redisRequestId, () -> {
                        var message = ofNullable(e.getMessage()).orElse("null");
                        log.error(new MapMessage<>(Map.of(
                                "point", "RedisClient.get.thrown",
                                "redis", Map.of(
                                        "thrown", message,
                                        "time", System.currentTimeMillis() - startMillis
                                ))));
                    });
                    return newRetryableException(e, resolve(), REDIS, e.getMessage());
                });
    }

    public Uni<List<Response>> batch(List<Request> requests) {
        var redisRequestId = UuidUtils.generateDefaultUuid();
        var startMillis = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            withRedisRequestId(redisRequestId, () -> log.debug(new MapMessage<>(Map.of(
                    "point", "RedisClient.batch.in",
                    "redis", Map.of(
                            "size", requests.size()
                    )))));
        }
        return redis.batch(requests)
                .onItem()
                .invoke(responses -> {
                    if (log.isDebugEnabled()) {
                        withRedisRequestId(redisRequestId, () -> log.debug(new MapMessage<>(Map.of(
                                "point", "RedisClient.batch.out",
                                "redis", Map.of(
                                        "result", responses.toString(),
                                        "time", System.currentTimeMillis() - startMillis
                                )))));
                    }
                })
                .onFailure()
                .transform(e -> {
                    withRedisRequestId(redisRequestId, () -> log.debug(new MapMessage<>(Map.of(
                            "point", "RedisClient.batch.thrown",
                            "redis", Map.of(
                                    "error", e.getMessage(),
                                    "time", System.currentTimeMillis() - startMillis
                            )))));
                    return newRetryableException(e, resolve(), REDIS, e.getMessage());
                });
    }

    private void withRedisRequestId(String redisRequestId, Runnable runnable) {
        try (var ignored = CloseableThreadContext.put("redisRequestId", redisRequestId)) {
            runnable.run();
        }
    }
}
