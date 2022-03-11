package ru.tsc.crm.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.tsc.crm.error.exception.BusinessException;
import ru.tsc.crm.error.exception.code.ExceptionCode;

@AllArgsConstructor
@Getter
public enum BusinessExceptionCode implements ExceptionCode<BusinessException> {
    METHOD_NOT_FOUND("01", "Метод не поддерживается api-gateway");

    String code;
    String message;
}
