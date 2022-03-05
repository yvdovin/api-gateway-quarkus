package ru.tsc.crm.constant;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ContextPropertyKey {

    public static final String SESSION_ID = "sessionId";
    public static final String DROP_SESSION = "dropSession";

}
