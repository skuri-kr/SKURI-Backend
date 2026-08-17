package com.skuri.skuri_backend.common.time;

import java.time.ZoneId;
import java.util.TimeZone;

public final class ApplicationTimeZone {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private ApplicationTimeZone() {
    }

    public static void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(SEOUL));
    }
}
