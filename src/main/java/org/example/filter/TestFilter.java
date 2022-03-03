package org.example.filter;

import lombok.RequiredArgsConstructor;
import org.example.service.redis.RedisClientAdapter;
import ru.tsc.crm.session.model.proto.SessionDataOuterClass;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.function.Consumer;

import static javax.ws.rs.Priorities.AUTHORIZATION;

@Priority(AUTHORIZATION)
@Provider
@Singleton
@RequiredArgsConstructor
public class TestFilter implements ContainerRequestFilter {

    private final RedisClientAdapter redisClientAdapter;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
//        redisClientAdapter.getSession("432da11e4acec811")
//                .onItem()
//                .invoke((Consumer<SessionDataOuterClass.SessionData>) System.out::println)
//                .subscribeAsCompletionStage()
//                .toCompletableFuture();
    }
}
