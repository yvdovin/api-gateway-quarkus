package ru.tsc.crm.service.redis;

import com.google.protobuf.util.JsonFormat;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;
import lombok.SneakyThrows;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.tsc.crm.session.SessionDataUtil;
import ru.tsc.crm.session.model.proto.SessionDataOuterClass;

import javax.inject.Singleton;
import java.util.List;

import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.SecurityExceptionCode.SESSION_DATA_NOT_FOUND;
import static ru.tsc.crm.error.exception.ExceptionFactory.newSecurityException;
import static ru.tsc.crm.session.RedisKeyPrefix.SESSIONS_IDS_BY_LOGIN;
import static ru.tsc.crm.session.RedisKeyPrefix.USER_DATA_BY_SESSION_ID;


@Singleton
public class RedisClientAdapter {

    private final RedisClient client;
    private final int sessionExpiry;

    public RedisClientAdapter(RedisClient client,
                              @ConfigProperty(name = "quarkus.redis.session-expiry", defaultValue = "900") int sessionExpiry) {
        this.client = client;
        this.sessionExpiry = sessionExpiry;
    }

    public Uni<SessionDataOuterClass.SessionData> refreshSession(String sessionId) {
        return client.get(USER_DATA_BY_SESSION_ID + sessionId, Response::toBytes, this::toPrettyString)
                .flatMap(sessionDataBytes -> {
                    if (sessionDataBytes == null) {
                        var exception = newSecurityException(resolve(), SESSION_DATA_NOT_FOUND, "sessionId='%s'".formatted(sessionId));
                        return Uni.createFrom().failure(() -> exception);
                    }
                    var login = SessionDataUtil.getSessionData(sessionDataBytes).getUser().getLogin();
                    var requests = List.of(
                            Request.cmd(Command.SET)
                                    .arg(SESSIONS_IDS_BY_LOGIN + login).arg(sessionId)
                                    .arg("EX").arg(String.valueOf(sessionExpiry)),
                            Request.cmd(Command.SET)
                                    .arg(USER_DATA_BY_SESSION_ID + sessionId).arg(sessionDataBytes)
                                    .arg("EX").arg(String.valueOf(sessionExpiry))
                    );
                    return client.batch(requests)
                            .map(u -> null);
                });
    }

    @SneakyThrows
    private String toPrettyString(byte[] bytes) {
        var sessionData = SessionDataUtil.getSessionData(bytes);
        return JsonFormat.printer().print(sessionData);
    }
}
