package com.example.testtaskgoalskkd.application;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Генератор идентификаторов вида {@code PREFIX-NNN}: следующий номер — максимальный
 * существующий + 1. Идентификаторы другого вида игнорируются.
 */
final class SequentialIds {

    private final String prefix;
    private final Pattern pattern;
    private long last;

    SequentialIds(String prefix, Collection<String> existingIds) {
        this.prefix = prefix;
        this.pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d+)$");
        for (String id : existingIds) {
            Matcher matcher = pattern.matcher(id);
            if (matcher.matches()) {
                last = Math.max(last, Long.parseLong(matcher.group(1)));
            }
        }
    }

    String next() {
        last++;
        return "%s%03d".formatted(prefix, last);
    }
}
