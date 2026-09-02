package com.mira.tags;

import com.mira.tags.api.MiraTagsApi;
import com.mira.tags.api.TagInfo;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.service.LuckPermsTagService;
import com.mira.tags.service.PlayerTagDataService;
import com.mira.tags.service.TagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class MiraTagsApiImpl implements MiraTagsApi {
    private final MiraTagsPlugin plugin;
    private final TagRegistry registry;
    private final PlayerTagDataService playerData;
    private final LuckPermsTagService tagService;

    public MiraTagsApiImpl(MiraTagsPlugin plugin, TagRegistry registry,
                           PlayerTagDataService playerData, LuckPermsTagService tagService) {
        this.plugin = plugin;
        this.registry = registry;
        this.playerData = playerData;
        this.tagService = tagService;
    }

    @Override
    public Collection<TagInfo> tags() {
        return registry.enabledTags().stream().map(this::info).toList();
    }

    @Override
    public Optional<TagInfo> tag(String id) {
        return registry.find(id).map(this::info);
    }

    @Override
    public boolean grant(UUID playerId, String tagId) {
        Optional<TagDefinition> tag = registry.find(tagId);
        if (tag.isEmpty()) return false;
        boolean changed = playerData.grant(playerId, tag.get().id());
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) tagService.applyActive(player);
        return changed;
    }

    @Override
    public boolean revoke(UUID playerId, String tagId) {
        Optional<TagDefinition> tag = registry.find(tagId);
        if (tag.isEmpty()) return false;
        boolean changed = playerData.revoke(playerId, tag.get().id());
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            tagService.applyActive(player);
        } else if (playerData.active(playerId).filter(tag.get().id()::equals).isPresent()
                && !tag.get().defaultUnlocked() && tag.get().permission().isBlank()) {
            playerData.clearActive(playerId);
        }
        return changed;
    }

    @Override
    public boolean owns(Player player, String tagId) {
        return tagService.owns(player, tagId);
    }

    @Override
    public Optional<String> activeTag(UUID playerId) {
        return playerData.active(playerId);
    }

    @Override
    public boolean equip(Player player, String tagId) {
        return tagService.equip(player, tagId);
    }

    @Override
    public void clear(Player player) {
        tagService.clear(player);
    }

    private TagInfo info(TagDefinition tag) {
        return new TagInfo(tag.id(), tag.displayName(), tag.suffix(), tag.icon().name(),
                tag.permission(), tag.defaultUnlocked(), tag.sortOrder());
    }
}
