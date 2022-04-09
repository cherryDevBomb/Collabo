package com.github.cherrydevbomb.collabo.communication.util;

import org.apache.commons.lang.RandomStringUtils;

public class UserIdGenerator {

    private static final int USER_ID_LENGTH = 7;

    public static String generate() {
        return RandomStringUtils.randomAlphabetic(USER_ID_LENGTH);
    }
}
