package ru.tsc.crm.error.exception;

import lombok.extern.log4j.Log4j2;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.SecurityExceptionCode.SESSION_ID_IS_ABSENT;
import static ru.tsc.crm.error.exception.ExceptionFactory.newInternalException;

@Provider
@Log4j2
public class HttpExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        final int status;
        final BaseException resultException;
        if (exception instanceof BaseException baseException) {
            resultException = baseException;
            if (baseException instanceof BusinessException) {
                status = 400;
            } else if (baseException instanceof SecurityException) {
                if (SESSION_ID_IS_ABSENT.getCode().equals(baseException.getCode())) {
                    status = 401;
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

        //var errorPayload = errorResponseDataBuilder.build(resultException, null);
        log.error("HttpExceptionHandler.toResponse.thrown {}", resultException.getFullErrorInfo(), resultException);
        return Response.status(status)
                .entity(resultException.getFullErrorInfo())
                .build();
    }

    public static BaseException mapException(Throwable e) {
        return e instanceof BaseException baseException
                ? baseException
                : newInternalException(e, resolve(), e.getMessage());
    }
}