package com.mira.tags.service;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.util.TagIds;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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

    public synchronized void reload() {
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

    public synchronized TagDefinition create(String requestedName, String requestedSuffix) throws IOException {
        String name = requestedName == null ? "" : requestedName.trim();
        String id = TagIds.normalize(name);
        if (id.isBlank()) throw new IllegalArgumentException("Tag name does not produce a valid id.");
        if (tags.containsKey(id)) throw new IllegalArgumentException("A tag with id '" + id + "' already exists.");

        String suffix = requestedSuffix == null ? "" : requestedSuffix.trim();
        if (suffix.isBlank()) throw new IllegalArgumentException("Tag suffix cannot be blank.");
        suffix = " " + suffix;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String path = "tags." + id;
        if (yaml.contains(path)) throw new IllegalArgumentException("A tag with id '" + id + "' already exists on disk.");

        yaml.set(path + ".enabled", true);
        yaml.set(path + ".display-name", "&f" + name);
        yaml.set(path + ".suffix", suffix);
        yaml.set(path + ".icon", "NAME_TAG");
        yaml.set(path + ".permission", "miratags.tag." + id);
        yaml.set(path + ".default-unlocked", false);
        yaml.set(path + ".sort-order", 100);
        yaml.set(path + ".description", List.of("&7Created in-game with &f/mtags add&7."));
        yaml.save(file);

        reload();
        return find(id).orElseThrow(() -> new IllegalStateException("Tag was saved but did not reload: " + id));
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
