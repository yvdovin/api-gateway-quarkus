package org.example.service.redis;

import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import lombok.RequiredArgsConstructor;

import javax.inject.Singleton;
import java.util.function.Consumer;
import java.util.function.Function;


@Singleton
@RequiredArgsConstructor
public class RedisClient {

    private final ReactiveRedisClient reactiveRedisClient;
    //private final Redis redis;

    public <T> Uni<T> get(String key, Function<Response, T> mapper, Function<T, String> logMapper) {
        //var redisRequestId = UuidUtils.generateDefaultUuid();
        //withRedisRequestId(redisRequestId, () -> log.debug("RedisClient.get.in key={}", key));
        return reactiveRedisClient.get(key)
                .map(response -> {
                    if (response != null) {
                        var result = mapper.apply(response);
//                        if (log.isTraceEnabled()) {
//                            var loggedResult = logMapper.apply(result);
//                            log.trace("RedisClient.get.out result={}", loggedResult);
//                        } else {
//                            log.debug("RedisClient.get.out");
//                        }
                        return result;
                    }
                    // log.warn("RedisClient.get.out result=null");
                    return null;
                })
                .onFailure()
                .invoke((Consumer<Throwable>) System.out::println);
        //.invoke(e -> withRedisRequestId(redisRequestId, () -> log.error("RedisClient.get.thrown {}", e.getMessage())));
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

//    private void withRedisRequestId(String redisRequestId, Runnable runnable) {
//        try (var ignored = CloseableThreadContext.put("redisRequestId", redisRequestId)) {
//            runnable.run();
//        }
//    }
}
