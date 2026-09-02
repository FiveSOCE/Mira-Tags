package com.mira.tags.service;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.util.TagIds;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
        return grants(playerId).contains(TagIds.normalize(tagId));
    }

    public Set<String> grants(UUID playerId) {
        return new LinkedHashSet<>(data.getStringList(path(playerId, "owned")));
    }

    public boolean grant(UUID playerId, String tagId) {
        String id = TagIds.normalize(tagId);
        Set<String> owned = grants(playerId);
        if (!owned.add(id)) return false;
        data.set(path(playerId, "owned"), List.copyOf(owned));
        save();
        return true;
    }

    public boolean revoke(UUID playerId, String tagId) {
        String id = TagIds.normalize(tagId);
        Set<String> owned = grants(playerId);
        if (!owned.remove(id)) return false;
        data.set(path(playerId, "owned"), List.copyOf(owned));
        save();
        return true;
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

    public void save() {
        try {
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + ex.getMessage());
        }
    }

    public File file() {
        return file;
    }

    private String path(UUID playerId, String child) {
        return "players." + playerId + "." + child;
    }
}
