package com.mira.tags.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private Text() {
    }

    public static Component component(String input) {
        return AMP.deserialize(input == null ? "" : input);
    }

    public static String section(String input) {
        return SECTION.serialize(component(input));
    }
}
