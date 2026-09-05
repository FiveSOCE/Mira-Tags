package com.mira.tags.service;

import com.mira.core.api.MilestoneService;
import com.mira.core.api.MiraCore;
import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.util.CosmeticsBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class MilestoneTagService {
    private final MiraTagsPlugin plugin;
    private final MiraCore core;
    private final TagRegistry registry;
    private final PlayerTagDataService data;
    private final LuckPermsTagService tags;
    private final Map<String, String> mappings = new HashMap<>();

    public MilestoneTagService(MiraTagsPlugin plugin, MiraCore core, TagRegistry registry,
                               PlayerTagDataService data, LuckPermsTagService tags) {
        this.plugin = plugin;
        this.core = core;
        this.registry = registry;
        this.data = data;
        this.tags = tags;
        mappings.put("mirafactions.ftop_champion", "ftop_champion");
        mappings.put("mirapinata.slayer", "pinata_slayer");
        mappings.put("miracrates.jackpot", "crate_jackpot");
        ensureBuiltin("FTop Champion", "&6[FTop Champion]");
        ensureBuiltin("Pinata Slayer", "&d[Pinata Slayer]");
        ensureBuiltin("Crate Jackpot", "&e[Jackpot]");
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            data.cleanupExpired();
            for (Player player : Bukkit.getOnlinePlayers()) sync(player);
            tags.refreshAll();
        }, 40L, 100L);
    }

    public void sync(Player player) {
        for (MilestoneService.Milestone milestone : core.milestones().all(player.getUniqueId())) {
            String mapped = mappings.get(milestone.key());
            if (mapped != null && data.grant(player.getUniqueId(), mapped)) {
                CosmeticsBridge.play(player, "tag_milestone_unlock");
            }
            if (milestone.key().startsWith("season.") && milestone.key().endsWith(".champion")) {
                String[] parts = milestone.key().split("\\.");
                if (parts.length >= 3) {
                    String id = "season_" + parts[1] + "_champion";
                    ensureBuiltin("Season " + parts[1] + " Champion", "&b[Season " + parts[1] + "]");
                    String created = registry.ids().stream().filter(tag -> tag.equals(id)).findFirst().orElse(null);
                    if (created != null && data.grant(player.getUniqueId(), created)) {
                        CosmeticsBridge.play(player, "tag_milestone_unlock");
                    }
                }
            }
        }
    }

    public boolean grantTimed(Player player, String tagId, Duration duration) {
        if (registry.find(tagId).isEmpty()) return false;
        boolean changed = data.grantTimed(player.getUniqueId(), tagId, duration);
        tags.applyActive(player);
        return changed;
    }

    private void ensureBuiltin(String name, String suffix) {
        String expected = name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        if (registry.find(expected).isPresent()) return;
        try {
            registry.create(name, suffix);
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getLogger().warning("Could not ensure achievement tag " + name + ": " + exception.getMessage());
        }
    }
}
