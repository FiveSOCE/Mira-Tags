package com.mira.tags.service;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.util.Text;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
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

    public void syncBackingGroups() {
        for (TagDefinition tag : registry.enabledTags()) {
            String permission = tag.permission();
            if (permission == null || permission.isBlank()) continue;

            String groupName = backingGroupName(tag.id());
            try {
                Group group = luckPerms.getGroupManager().createAndLoadGroup(groupName).join();
                group.data().clear(NodeType.PERMISSION.predicate(node ->
                        node.getKey().startsWith("miratags.tag.")));
                group.data().add(PermissionNode.builder(permission).value(true).build());
                luckPerms.getGroupManager().saveGroup(group).join();
            } catch (RuntimeException exception) {
                plugin.getLogger().severe("Could not sync LuckPerms backing group '" + groupName
                        + "' for tag '" + tag.id() + "': " + exception.getMessage());
            }
        }
    }

    public String backingGroupName(String tagId) {
        String normalized = tagId == null ? "" : tagId.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
        return "miratag_" + normalized;
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

        clearManagedSuffix(player);
        if (!dispatchSuffixCommand(player, found.get())) return false;

        playerData.setActive(player.getUniqueId(), found.get().id());
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

        clearManagedSuffix(user);
        if (!dispatchSuffixCommand(player, tag)) {
            plugin.getLogger().warning("LuckPerms did not accept MiraTag suffix command for " + player.getName()
                    + " and tag " + tag.id());
        }
    }

    private boolean dispatchSuffixCommand(Player player, TagDefinition tag) {
        String suffix = tag.suffix();
        if (suffix == null || suffix.isBlank()) return false;
        if (suffix.indexOf('"') >= 0 || suffix.indexOf('\n') >= 0 || suffix.indexOf('\r') >= 0) {
            plugin.getLogger().severe("Refusing unsafe LuckPerms suffix command for tag '" + tag.id()
                    + "': suffix contains quotes or line breaks.");
            return false;
        }

        String command = "lp user " + player.getName() + " meta addsuffix 0 \"" + suffix + "\"";
        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (dispatched) {
            plugin.getLogger().fine("Applied MiraTag '" + tag.id() + "' through console: " + command);
        }
        return dispatched;
    }

    private void clearManagedSuffix(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;
        clearManagedSuffix(user);
        luckPerms.getUserManager().saveUser(user);
    }

    private void clearManagedSuffix(User user) {
        user.data().clear(NodeType.SUFFIX.predicate(node -> isMiraTagSuffix(node.getPriority(), node.getMetaValue())));
        luckPerms.getUserManager().saveUser(user);
    }

    private boolean isMiraTagSuffix(int priority, String value) {
        if (priority != 0 && priority != 500) return false;
        if (value == null) return false;

        return registry.enabledTags().stream().anyMatch(tag ->
                value.equals(tag.suffix()) || value.equals(Text.section(tag.suffix())));
    }
}
