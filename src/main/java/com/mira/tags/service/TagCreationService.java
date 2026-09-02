package com.mira.tags.service;

import com.mira.core.api.MiraCore;
import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.util.TagIds;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TagCreationService {
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_SUFFIX_LENGTH = 128;

    private final MiraTagsPlugin plugin;
    private final MiraCore core;
    private final TagRegistry registry;
    private final Map<UUID, PendingTag> pending = new ConcurrentHashMap<>();

    public TagCreationService(MiraTagsPlugin plugin, MiraCore core, TagRegistry registry) {
        this.plugin = plugin;
        this.core = core;
        this.registry = registry;
    }

    public void begin(Player player, String requestedName) {
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isBlank()) {
            core.messages().send(player, "&eUsage: /mtags add <Tag Name>");
            return;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            core.messages().send(player, "&cTag names can be at most " + MAX_NAME_LENGTH + " characters.");
            return;
        }

        String id = TagIds.normalize(name);
        if (id.isBlank()) {
            core.messages().send(player, "&cThat name cannot be converted into a valid tag id.");
            return;
        }
        if (registry.find(id).isPresent()) {
            core.messages().send(player, "&cA tag with id &f" + id + " &calready exists.");
            return;
        }

        pending.put(player.getUniqueId(), new PendingTag(name, id));
        core.messages().send(player, "&dCreating tag: &f" + name);
        core.messages().send(player, "&eEnter the tag format in chat. &7Example: &f&8[&eKing&8]");
        core.messages().send(player, "&7Your message will not be broadcast. Type &fcancel &7to abort.");
    }

    public boolean awaiting(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void cancel(UUID playerId) {
        pending.remove(playerId);
    }

    public void handleChat(Player player, String rawInput) {
        PendingTag pendingTag = pending.get(player.getUniqueId());
        if (pendingTag == null) return;

        String input = rawInput == null ? "" : rawInput.trim();
        if (input.equalsIgnoreCase("cancel")) {
            pending.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin,
                    () -> core.messages().send(player, "&eTag creation cancelled."));
            return;
        }

        if (input.isBlank()) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> core.messages().send(player, "&cTag format cannot be blank. Type a format or &fcancel&c."));
            return;
        }
        if (input.length() > MAX_SUFFIX_LENGTH) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> core.messages().send(player, "&cTag format can be at most " + MAX_SUFFIX_LENGTH + " characters."));
            return;
        }

        pending.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> create(player, pendingTag, input));
    }

    private void create(Player player, PendingTag pendingTag, String suffix) {
        try {
            TagDefinition created = registry.create(pendingTag.displayName(), suffix);
            core.messages().send(player, "&aCreated tag &f" + created.displayName() + " &7(&f" + created.id() + "&7).");
            core.messages().send(player, "&7Suffix: &f" + suffix);
            core.messages().send(player, "&7LuckPerms permission: &f" + created.permission());
            core.messages().send(player, "&7Grant with &f/mtags grant <player> " + created.id());
        } catch (IllegalArgumentException exception) {
            core.messages().send(player, "&cCould not create tag: " + exception.getMessage());
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save tag '" + pendingTag.id() + "': " + exception.getMessage());
            core.messages().send(player, "&cCould not save the tag to tags.yml. Check console for details.");
        }
    }

    private record PendingTag(String displayName, String id) {
    }
}
