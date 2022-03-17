package ru.tsc.crm.error.exception;

import lombok.extern.log4j.Log4j2;
import org.jboss.resteasy.spi.HttpRequest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.SecurityExceptionCode.*;
import static ru.tsc.crm.error.exception.ExceptionFactory.newInternalException;
import static ru.tsc.crm.util.http.HttpUtils.createDropSessionIdCookie;

/**
 * Хендлер для обработки исключений. Логирует исключения и формирует ответ клиенту.
 */
@Log4j2
public class HttpExceptionHandler {

    public static Response toResponse(Throwable exception, HttpRequest request) {
        Response.ResponseBuilder entity = Response.noContent();
        final int status;
        final BaseException resultException;
        if (exception instanceof BaseException baseException) {
            resultException = baseException;
            if (baseException instanceof BusinessException) {
                status = 400;
            } else if (baseException instanceof SecurityException) {
                String exceptionCode = baseException.getCode();
                if (SESSION_ID_IS_ABSENT.getCode().equals(exceptionCode)) {
                    status = 401;
                } else if (SESSION_DATA_NOT_FOUND.getCode().equals(exceptionCode)) {
                    status = 403;
                    entity.header(HttpHeaders.SET_COOKIE, createDropSessionIdCookie(request));
                } else if (METHOD_ACCESS_DENIED.getCode().equals(exceptionCode) ||
                        SUB_METHOD_ACCESS_DENIED.getCode().equals(exceptionCode)) {
                    status = 403;
                } else {
                    status = 500;
                }
            } else {
                status = 500;
            }
        } else if (exception instanceof WebApplicationException webApplicationException) {
            status = webApplicationException.getResponse().getStatus();
            resultException = newInternalException(webApplicationException, resolve(), webApplicationException.getMessage());
        } else {
            status = 500;
            resultException = newInternalException(exception, resolve(), exception.getMessage());
        }

        log.error("HttpExceptionHandler.toResponse.thrown {}", resultException.getFullErrorInfo(), resultException);
        entity.status(status)
                .entity(resultException.getFullErrorInfo());
        return entity
                .build();
    }
}
