package com.fptu.swp391.sportscentermanager.service.ai;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AiToolUtils {
    private AiToolUtils() {}

    public static Map<String, Object> row(Object...keyValues){
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            map.put((String) keyValues[i], value == null ? null : value.toString());
        }
        return map;
    }
}
