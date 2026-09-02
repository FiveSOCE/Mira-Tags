package com.mira.tags.service;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.util.TagIds;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TagRegistry {
    private final MiraTagsPlugin plugin;
    private final File file;
    private final Map<String, TagDefinition> tags = new LinkedHashMap<>();

    public TagRegistry(MiraTagsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tags.yml");
        reload();
    }

    public void reload() {
        tags.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("tags");
        if (section == null) return;

        for (String rawId : section.getKeys(false)) {
            ConfigurationSection tag = section.getConfigurationSection(rawId);
            if (tag == null || !tag.getBoolean("enabled", true)) continue;

            String id = TagIds.normalize(rawId);
            if (id.isBlank()) {
                plugin.getLogger().warning("Ignoring tag with invalid id: " + rawId);
                continue;
            }

            Material icon = Material.matchMaterial(tag.getString("icon", "NAME_TAG"));
            if (icon == null || icon.isAir()) icon = Material.NAME_TAG;

            String displayName = tag.getString("display-name", "&f" + id);
            String suffix = tag.getString("suffix", " &7[" + id + "]");
            String permission = tag.getString("permission", "").trim();
            boolean defaultUnlocked = tag.getBoolean("default-unlocked", false);
            int sortOrder = tag.getInt("sort-order", 100);
            List<String> description = new ArrayList<>(tag.getStringList("description"));

            tags.put(id, new TagDefinition(id, displayName, suffix, icon, permission,
                    defaultUnlocked, sortOrder, List.copyOf(description)));
        }
    }

    public Optional<TagDefinition> find(String id) {
        return Optional.ofNullable(tags.get(TagIds.normalize(id)));
    }

    public Collection<TagDefinition> enabledTags() {
        return tags.values().stream()
                .sorted(Comparator.comparingInt(TagDefinition::sortOrder).thenComparing(TagDefinition::id))
                .toList();
    }

    public Collection<String> ids() {
        return enabledTags().stream().map(TagDefinition::id).toList();
    }
}
