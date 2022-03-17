package ru.tsc.crm.error.exception;

import lombok.extern.log4j.Log4j2;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static ru.tsc.crm.error.ModuleOperationCode.resolve;
import static ru.tsc.crm.error.SecurityExceptionCode.*;
import static ru.tsc.crm.error.SecurityExceptionCode.SUB_METHOD_ACCESS_DENIED;
import static ru.tsc.crm.error.exception.ExceptionFactory.newInternalException;
import static ru.tsc.crm.util.http.HttpUtils.createDropSessionIdCookie;

@Provider
@Log4j2
public class HttpExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        final int status;
        final BaseException resultException;
        Response.ResponseBuilder responseBuilder = Response.noContent();
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
                    responseBuilder.header(HttpHeaders.SET_COOKIE, createDropSessionIdCookie(baseException.getDetails()));
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

        //var errorPayload = errorResponseDataBuilder.build(resultException, null);
        log.error("HttpExceptionHandler.toResponse.thrown {}", resultException.getFullErrorInfo(), resultException);
        return responseBuilder.status(status)
                .entity(resultException.getFullErrorInfo())
                .build();
    }

}