package com.mira.tags.api;

public record TagInfo(
        String id,
        String displayName,
        String suffix,
        String iconMaterial,
        String permission,
        boolean defaultUnlocked,
        int sortOrder
) {
}
