package com.mira.tags.api;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MiraTagsApi {
    Collection<TagInfo> tags();

    Optional<TagInfo> tag(String id);

    boolean grant(UUID playerId, String tagId);

    boolean revoke(UUID playerId, String tagId);

    boolean owns(Player player, String tagId);

    Optional<String> activeTag(UUID playerId);

    boolean equip(Player player, String tagId);

    void clear(Player player);
}
