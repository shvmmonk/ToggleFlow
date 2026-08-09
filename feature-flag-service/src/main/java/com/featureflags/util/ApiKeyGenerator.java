package com.featureflags.util;

import java.security.SecureRandom;
import java.util.HexFormat;

public class ApiKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PREFIX = "ff_live_";

    public static String generateApiKey() {
        byte[] randomBytes = new byte[28]; // 28 bytes = 56 hex chars + "ff_live_" (8 chars) = 64 chars
        SECURE_RANDOM.nextBytes(randomBytes);
        return PREFIX + HexFormat.of().formatHex(randomBytes);
    }
}
