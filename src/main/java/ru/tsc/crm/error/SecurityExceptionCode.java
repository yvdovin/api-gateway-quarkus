package ru.tsc.crm.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.tsc.crm.error.exception.SecurityException;
import ru.tsc.crm.error.exception.code.ExceptionCode;



@AllArgsConstructor
@Getter
public enum SecurityExceptionCode implements ExceptionCode<SecurityException> {

    SESSION_ID_IS_ABSENT("01", "Кука  sessionId отсутствует"),
    SESSION_DATA_NOT_FOUND("02", "Данные сессии не найдены"),
    METHOD_ACCESS_DENIED("02", "Вызываемый метод не связан с Ролью Пользователя"),
    SUB_METHOD_ACCESS_DENIED("03", "Вызываемый подметод не связан с Ролью Пользователя"),
    PROVIDER_METHOD_SERVICE("04", "Недостаточно прав для вызова provider-method-service");

    private final String code;
    private final String message;
    }
