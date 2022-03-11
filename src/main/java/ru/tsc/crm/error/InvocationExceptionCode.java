package ru.tsc.crm.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.tsc.crm.error.exception.InvocationException;
import ru.tsc.crm.error.exception.code.ExceptionCode;

@AllArgsConstructor
@Getter
public enum InvocationExceptionCode implements ExceptionCode<InvocationException> {

    REDIS("01", "Ошибка при вызове редиса"),
    PROVIDER_METHOD_SERVICE("02", "Ошибка при вызове провайдер сервиса");

    private final String code;
    private final String message;
}
