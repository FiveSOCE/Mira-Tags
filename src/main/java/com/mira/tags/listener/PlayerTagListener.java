package com.mira.tags.listener;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.service.LuckPermsTagService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerTagListener implements Listener {
    private final MiraTagsPlugin plugin;
    private final LuckPermsTagService tagService;

    public PlayerTagListener(MiraTagsPlugin plugin, LuckPermsTagService tagService) {
        this.plugin = plugin;
        this.tagService = tagService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) tagService.applyActive(event.getPlayer());
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tagService.removeApplied(event.getPlayer());
    }
}
