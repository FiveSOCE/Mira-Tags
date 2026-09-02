package com.mira.tags.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagIdsTest {
    @Test
    void normalizesHumanNames() {
        assertEquals("pvp_master", TagIds.normalize("PvP Master"));
    }

    @Test
    void removesUnsafeCharacters() {
        assertEquals("og-tag", TagIds.normalize(" OG-Tag! "));
    }
}
