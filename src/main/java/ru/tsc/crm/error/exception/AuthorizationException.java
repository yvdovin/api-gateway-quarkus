package ru.tsc.crm.error.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

@Accessors(chain = true)
public class AuthorizationException extends RuntimeException {

    @Getter
    private final int status;
    @Getter
    @Setter
    private String originalCode;
    @Getter
    @Setter
    private String originalMessage;
    @Getter
    private String[] details;

    private String message;
    @Getter
    private Throwable cause;

    public AuthorizationException(int status) {
        this.status = status;
    }

    public AuthorizationException(int status, @NotNull String message, String... details) {
        super(message);
        this.status = status;
        this.message = message;
        this.details = details;
    }

    public AuthorizationException(int status, @NotNull Throwable cause, String... details) {
        super(cause);
        this.status = status;
        this.message = cause.getMessage();
        this.cause = cause;
        this.details = details;
    }

    public AuthorizationException(int status, @NotNull String message, @NotNull Throwable cause, String... details) {
        super(message, cause);
        this.status = status;
        this.message = resolveMessage(message, cause);
        this.cause = cause;
        this.details = details;
    }

    public static AuthorizationException notAuthorizedException() {
        return new AuthorizationException(401);
    }

    public static AuthorizationException notAuthorizedException(String message, String... exceptionDetails) {
        return new AuthorizationException(401, message, exceptionDetails);
    }

    public static AuthorizationException notAuthorizedException(String message, Throwable cause, String... exceptionDetails) {
        return new AuthorizationException(401, message, cause, exceptionDetails);
    }

    public static AuthorizationException forbiddenException() {
        return new AuthorizationException(403);
    }

    public static AuthorizationException forbiddenException(String message, String... exceptionDetails) {
        return new AuthorizationException(403, message, exceptionDetails);
    }

    public static AuthorizationException forbiddenException(String message, Throwable cause, String... exceptionDetails) {
        return new AuthorizationException(403, message, cause, exceptionDetails);
    }

    private String resolveMessage(String message, Throwable cause) {
        return new StringJoiner(";").add(message).add(cause.getMessage()).toString();
    }

    @Override
    public String getMessage() {
        return this.message != null
                ? this.message + "; status=" + this.status
                : "status=" + status;
    }

    public String[] getMessageWithDetails() {
        if (details == null || details.length == 0) {
            return new String[]{getMessage()};
        }
        var result = new String[details.length + 1];
        result[0] = message;
        System.arraycopy(details, 0, result, 1, details.length);
        return result;
    }
}
