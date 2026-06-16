package com.iskollect.util;

import java.util.UUID;

public final class CouponGenerator {
    private CouponGenerator() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
    }
}
