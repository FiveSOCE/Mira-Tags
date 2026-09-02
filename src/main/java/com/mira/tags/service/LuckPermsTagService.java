package com.mira.tags.service;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.util.Text;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class LuckPermsTagService {
    private final MiraTagsPlugin plugin;
    private final TagRegistry registry;
    private final PlayerTagDataService playerData;
    private final LuckPerms luckPerms;

    public LuckPermsTagService(MiraTagsPlugin plugin, TagRegistry registry, PlayerTagDataService playerData) {
        this.plugin = plugin;
        this.registry = registry;
        this.playerData = playerData;
        this.luckPerms = LuckPermsProvider.get();
    }

    public boolean owns(Player player, TagDefinition tag) {
        return tag.defaultUnlocked()
                || playerData.granted(player.getUniqueId(), tag.id())
                || (!tag.permission().isBlank() && player.hasPermission(tag.permission()));
    }

    public boolean owns(Player player, String tagId) {
        return registry.find(tagId).map(tag -> owns(player, tag)).orElse(false);
    }

    public boolean equip(Player player, String tagId) {
        Optional<TagDefinition> found = registry.find(tagId);
        if (found.isEmpty() || !owns(player, found.get())) return false;
        playerData.setActive(player.getUniqueId(), found.get().id());
        apply(player, found.get());
        return true;
    }

    public void clear(Player player) {
        clearManagedSuffix(player);
        playerData.clearActive(player.getUniqueId());
    }

    public void applyActive(Player player) {
        Optional<String> active = playerData.active(player.getUniqueId());
        if (active.isEmpty()) {
            clearManagedSuffix(player);
            return;
        }

        Optional<TagDefinition> found = registry.find(active.get());
        if (found.isEmpty() || !owns(player, found.get())) {
            clear(player);
            return;
        }
        apply(player, found.get());
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) applyActive(player);
    }

    public void removeApplied(Player player) {
        clearManagedSuffix(player);
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) clearManagedSuffix(player);
    }

    private void apply(Player player, TagDefinition tag) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) applyActive(player);
            }, 1L);
            return;
        }

        clearAtReservedPriority(user);
        user.data().add(SuffixNode.builder(Text.section(tag.suffix()), plugin.suffixPriority()).build());
        luckPerms.getUserManager().saveUser(user);
    }

    private void clearManagedSuffix(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;
        clearAtReservedPriority(user);
        luckPerms.getUserManager().saveUser(user);
    }

    private void clearAtReservedPriority(User user) {
        int priority = plugin.suffixPriority();
        user.data().clear(NodeType.SUFFIX.predicate(node -> node.getPriority() == priority));
    }
}
