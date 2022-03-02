package org.example.service.redis;

import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.message.MapMessage;
import ru.tsc.crm.quarkus.common.uuid.UuidUtils;

import javax.inject.Singleton;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

//TODO перенести это все в утил либу
@Singleton
@RequiredArgsConstructor
@Log4j2
public class RedisClient {

    private final ReactiveRedisClient reactiveRedisClient;
    //private final Redis redis;

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
        return reactiveRedisClient.get(key)
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
                .invoke(e -> withRedisRequestId(redisRequestId, () -> {
                    var message = ofNullable(e.getMessage()).orElse("null");
                    log.error(new MapMessage<>(Map.of(
                            "point", "RedisClient.get.thrown",
                            "redis", Map.of(
                                    "thrown", message,
                                    "time", System.currentTimeMillis() - startMillis
                            ))));
                }));
    }

//    public Uni<List<Response>> batch(List<Request> requests) {
//        //var redisRequestId = UuidUtils.generateDefaultUuid();
//        var point = "RedisClient.batch";
//        //withRedisRequestId(redisRequestId, () -> log.debug(point + ".in size={}", requests.size()));
//        return redis.batch(requests)
//                .onItem()
//                .invoke(responses -> withRedisRequestId(redisRequestId, () -> log.debug(point + ".out result={}", responses.toString())))
//                .onFailure()
//                .invoke(e -> withRedisRequestId(redisRequestId, () -> log.error(point + ".thrown {}", e.getMessage())));
//    }

    private void withRedisRequestId(String redisRequestId, Runnable runnable) {
        try (var ignored = CloseableThreadContext.put("redisRequestId", redisRequestId)) {
            runnable.run();
        }
    }
}
