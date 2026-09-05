package com.mira.tags.service;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.util.TagIds;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PlayerTagDataService {
    private final MiraTagsPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public PlayerTagDataService(MiraTagsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create MiraTags data folder.");
        }
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) plugin.getLogger().warning("Could not create playerdata.yml");
            } catch (IOException ex) {
                throw new IllegalStateException("Could not create playerdata.yml", ex);
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean granted(UUID playerId, String tagId) {
        String id = TagIds.normalize(tagId);
        if (!grants(playerId).contains(id)) return false;
        long expiresAt = data.getLong(path(playerId, "expires." + id), 0L);
        if (expiresAt > 0L && System.currentTimeMillis() >= expiresAt) {
            revoke(playerId, id);
            return false;
        }
        return true;
    }

    public Set<String> grants(UUID playerId) {
        Set<String> owned = new LinkedHashSet<>(data.getStringList(path(playerId, "owned")));
        boolean changed = owned.removeIf(id -> {
            long expiresAt = data.getLong(path(playerId, "expires." + TagIds.normalize(id)), 0L);
            return expiresAt > 0L && System.currentTimeMillis() >= expiresAt;
        });
        if (changed) {
            data.set(path(playerId, "owned"), List.copyOf(owned));
            save();
        }
        return owned;
    }

    public boolean grant(UUID playerId, String tagId) {
        String id = TagIds.normalize(tagId);
        Set<String> owned = grants(playerId);
        boolean added = owned.add(id);
        data.set(path(playerId, "owned"), List.copyOf(owned));
        data.set(path(playerId, "expires." + id), null);
        save();
        return added;
    }

    public boolean grantTimed(UUID playerId, String tagId, Duration duration) {
        String id = TagIds.normalize(tagId);
        if (duration == null || duration.isZero() || duration.isNegative()) return grant(playerId, id);
        Set<String> owned = grants(playerId);
        boolean added = owned.add(id);
        data.set(path(playerId, "owned"), List.copyOf(owned));
        data.set(path(playerId, "expires." + id), Instant.now().plus(duration).toEpochMilli());
        save();
        return added;
    }

    public Optional<Instant> expiresAt(UUID playerId, String tagId) {
        long value = data.getLong(path(playerId, "expires." + TagIds.normalize(tagId)), 0L);
        return value <= 0L ? Optional.empty() : Optional.of(Instant.ofEpochMilli(value));
    }

    public boolean revoke(UUID playerId, String tagId) {
        String id = TagIds.normalize(tagId);
        Set<String> owned = new LinkedHashSet<>(data.getStringList(path(playerId, "owned")));
        boolean removed = owned.remove(id);
        data.set(path(playerId, "owned"), List.copyOf(owned));
        data.set(path(playerId, "expires." + id), null);
        if (active(playerId).orElse("").equals(id)) data.set(path(playerId, "active"), null);
        if (removed) save();
        return removed;
    }

    public Optional<String> active(UUID playerId) {
        String active = TagIds.normalize(data.getString(path(playerId, "active"), ""));
        return active.isBlank() ? Optional.empty() : Optional.of(active);
    }

    public void setActive(UUID playerId, String tagId) {
        data.set(path(playerId, "active"), TagIds.normalize(tagId));
        save();
    }

    public void clearActive(UUID playerId) {
        data.set(path(playerId, "active"), null);
        save();
    }

    public TagDeletionResult purgeTag(String tagId) {
        String id = TagIds.normalize(tagId);
        int grantsRemoved = 0;
        int selectionsCleared = 0;
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return new TagDeletionResult(0, 0);
        for (String playerKey : players.getKeys(false)) {
            String base = "players." + playerKey;
            List<String> owned = data.getStringList(base + ".owned");
            List<String> cleaned = owned.stream().filter(value -> !TagIds.normalize(value).equals(id)).toList();
            if (cleaned.size() != owned.size()) {
                grantsRemoved += owned.size() - cleaned.size();
                data.set(base + ".owned", cleaned);
            }
            data.set(base + ".expires." + id, null);
            String active = TagIds.normalize(data.getString(base + ".active", ""));
            if (active.equals(id)) {
                data.set(base + ".active", null);
                selectionsCleared++;
            }
        }
        if (grantsRemoved > 0 || selectionsCleared > 0) save();
        return new TagDeletionResult(grantsRemoved, selectionsCleared);
    }

    public void cleanupExpired() {
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return;
        for (String raw : players.getKeys(false)) {
            try { grants(UUID.fromString(raw)); } catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        try { data.save(file); }
        catch (IOException ex) { plugin.getLogger().severe("Could not save playerdata.yml: " + ex.getMessage()); }
    }

    public File file() { return file; }
    private String path(UUID playerId, String child) { return "players." + playerId + "." + child; }
}
