package org.example.service.redis;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.util.JsonFormat;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.tsc.crm.session.model.proto.SessionDataOuterClass;

import javax.inject.Singleton;

import static ru.tsc.crm.session.RedisKeyPrefix.USER_DATA_BY_SESSION_ID;

@RequiredArgsConstructor
@Singleton
public class RedisClientAdapter {

    private final RedisClient client;

    public Uni<SessionDataOuterClass.SessionData> getSession(String sessionId) {
        return client.get(USER_DATA_BY_SESSION_ID + sessionId, this::getSessionData, this::toPrettyString)
                .onItem()
                .ifNull()
                .failWith(() -> new RuntimeException("Key: '%s'".formatted(USER_DATA_BY_SESSION_ID + sessionId)));
    }

    @SneakyThrows
    private SessionDataOuterClass.SessionData getSessionData(io.vertx.mutiny.redis.client.Response response) {
        var bytes = response.toBytes();
        return SessionDataOuterClass.SessionData.parseFrom(bytes);
    }

    @SneakyThrows
    private <T extends GeneratedMessageV3> String toPrettyString(T entity) {
        return JsonFormat.printer().print(entity);
    }
}
