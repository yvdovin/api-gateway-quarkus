package ru.tsc.crm.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.logging.log4j.ThreadContext;
import ru.tsc.crm.error.operation.OperationCode;
import ru.tsc.crm.quarkus.common.constant.MdcKey;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@AllArgsConstructor
@Getter
public enum ModuleOperationCode implements OperationCode {

    BASE_OPERATION("01", "baseOperation");

    private static final Map<String, OperationCode> valuesByName = Stream.of(ModuleOperationCode.values())
            .collect(Collectors.toMap(ModuleOperationCode::getName, Function.identity()));

    String code;
    String name;

    public static OperationCode resolve() {
        return ofNullable(valuesByName.get(ThreadContext.get(MdcKey.OPERATION_NAME))).orElse(() -> OTHER);
    }

    public void init() {
        ThreadContext.put(MdcKey.OPERATION_NAME, this.name);
    }

}
