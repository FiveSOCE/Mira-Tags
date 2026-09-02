package com.mira.tags.util;

import java.util.Locale;

public final class TagIds {
    private TagIds() {
    }

    public static String normalize(String input) {
        if (input == null) return "";
        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_-]", "")
                .replaceAll("_+", "_");
    }
}
