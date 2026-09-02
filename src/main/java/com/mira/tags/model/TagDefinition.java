package com.mira.tags.model;

import org.bukkit.Material;

import java.util.List;

public record TagDefinition(
        String id,
        String displayName,
        String suffix,
        Material icon,
        String permission,
        boolean defaultUnlocked,
        int sortOrder,
        List<String> description
) {
}
