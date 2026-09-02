package com.mira.tags.command;

import com.mira.core.api.MiraCore;
import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.service.LuckPermsTagService;
import com.mira.tags.service.PlayerTagDataService;
import com.mira.tags.service.TagCreationService;
import com.mira.tags.service.TagDeletionResult;
import com.mira.tags.service.TagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class MiraTagsAdminCommand implements CommandExecutor, TabCompleter {
    private final MiraTagsPlugin plugin;
    private final MiraCore core;
    private final TagRegistry registry;
    private final PlayerTagDataService playerData;
    private final LuckPermsTagService tagService;
    private final TagCreationService creation;

    public MiraTagsAdminCommand(MiraTagsPlugin plugin, MiraCore core, TagRegistry registry,
                                PlayerTagDataService playerData, LuckPermsTagService tagService,
                                TagCreationService creation) {
        this.plugin = plugin;
        this.core = core;
        this.registry = registry;
        this.playerData = playerData;
        this.tagService = tagService;
        this.creation = creation;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> add(sender, args);
            case "delete" -> delete(sender, args);
            case "grant" -> grant(sender, args);
            case "revoke" -> revoke(sender, args);
            case "clear" -> clear(sender, args);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "test" -> test(sender);
            case "help" -> help(sender);
            default -> help(sender);
        }
        return true;
    }

    private void add(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            core.messages().send(sender, "&c/mtags add must be started by a player because the tag format is entered in chat.");
            return;
        }
        if (args.length < 2) {
            core.messages().send(sender, "&eUsage: /mtags add <Tag Name>");
            return;
        }
        creation.begin(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            core.messages().send(sender, "&eUsage: /mtag delete <tag>");
            return;
        }

        String requested = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        TagDefinition tag = registry.find(requested).orElse(null);
        if (tag == null) {
            core.messages().send(sender, "&cUnknown tag: " + requested);
            return;
        }

        try {
            if (!registry.delete(tag.id())) {
                core.messages().send(sender, "&cCould not delete tag &f" + tag.id() + "&c from tags.yml.");
                return;
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not delete tag '" + tag.id() + "': " + ex.getMessage());
            core.messages().send(sender, "&cCould not save tags.yml while deleting that tag.");
            return;
        }

        TagDeletionResult cleanup = playerData.purgeTag(tag.id());
        tagService.refreshAll();

        core.messages().send(sender, "&aDeleted tag &f" + tag.id() + "&a.");
        core.messages().send(sender, "&7Cleaned &f" + cleanup.grantsRemoved() + "&7 saved grant(s) and &f"
                + cleanup.selectionsCleared() + "&7 active selection(s). External LuckPerms permission assignments were left untouched.");
    }

    private void grant(CommandSender sender, String[] args) {
        if (args.length < 3) {
            core.messages().send(sender, "&eUsage: /mtags grant <player> <tag>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            core.messages().send(sender, "&cThat player must be online.");
            return;
        }
        TagDefinition tag = registry.find(args[2]).orElse(null);
        if (tag == null) {
            core.messages().send(sender, "&cUnknown tag: " + args[2]);
            return;
        }
        boolean changed = playerData.grant(target.getUniqueId(), tag.id());
        core.messages().send(sender, changed
                ? "&aGranted &f" + tag.id() + " &ato &f" + target.getName() + "&a."
                : "&e" + target.getName() + " already owns the internal grant for " + tag.id() + ".");
    }

    private void revoke(CommandSender sender, String[] args) {
        if (args.length < 3) {
            core.messages().send(sender, "&eUsage: /mtags revoke <player> <tag>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            core.messages().send(sender, "&cThat player must be online.");
            return;
        }
        TagDefinition tag = registry.find(args[2]).orElse(null);
        if (tag == null) {
            core.messages().send(sender, "&cUnknown tag: " + args[2]);
            return;
        }
        boolean changed = playerData.revoke(target.getUniqueId(), tag.id());
        tagService.applyActive(target);
        core.messages().send(sender, changed
                ? "&aRevoked the internal grant for &f" + tag.id() + " &afrom &f" + target.getName() + "&a."
                : "&e" + target.getName() + " did not have an internal grant for " + tag.id() + ".");
    }

    private void clear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            core.messages().send(sender, "&eUsage: /mtags clear <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            core.messages().send(sender, "&cThat player must be online.");
            return;
        }
        tagService.clear(target);
        core.messages().send(sender, "&aCleared the active tag for &f" + target.getName() + "&a.");
    }

    private void list(CommandSender sender) {
        Collection<TagDefinition> tags = registry.enabledTags();
        core.messages().send(sender, "&dMiraTags &7- &f" + tags.size() + " enabled tag(s)");
        if (tags.isEmpty()) {
            core.messages().send(sender, "&7No enabled tags are currently defined in tags.yml.");
            return;
        }
        core.messages().send(sender, "&7" + String.join(", ", tags.stream().map(TagDefinition::id).toList()));
    }

    private void reload(CommandSender sender) {
        plugin.reloadPluginConfiguration();
        core.messages().send(sender, "&aMiraTags configuration, definitions and player data reloaded.");
    }

    private void test(CommandSender sender) {
        int passed = 0;
        int total = 7;
        if (plugin.isEnabled()) passed++;
        if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) passed++;
        if (plugin.getServer().getPluginManager().isPluginEnabled("MiraCore")) passed++;
        if (new File(plugin.getDataFolder(), "tags.yml").isFile()) passed++;
        if (playerData.file().isFile()) passed++;
        if (plugin.suffixPriority() >= 0) passed++;
        if (plugin.getCommand("tags") != null && plugin.getCommand("miratags") != null) passed++;

        String color = passed == total ? "&a" : "&c";
        core.messages().send(sender, color + "MiraTags Self-Test: " + passed + "/" + total + " passed.");
        core.messages().send(sender, "&7Enabled definitions: &f" + registry.enabledTags().size()
                + "&7 | LuckPerms suffix priority: &f" + plugin.suffixPriority());
    }

    private void help(CommandSender sender) {
        core.messages().send(sender, "&dMiraTags Admin");
        core.messages().send(sender, "&f/tags &7- open the player tag selector");
        core.messages().send(sender, "&f/mtags add <Tag Name> &7- create a tag using private chat input");
        core.messages().send(sender, "&f/mtag delete <tag> &7- permanently delete a tag and clean MiraTags player data");
        core.messages().send(sender, "&f/mtags grant <player> <tag> &7- permanently unlock a tag");
        core.messages().send(sender, "&f/mtags revoke <player> <tag> &7- remove an internal unlock");
        core.messages().send(sender, "&f/mtags clear <player> &7- clear a player's active tag");
        core.messages().send(sender, "&f/mtags list &7- list enabled tag ids");
        core.messages().send(sender, "&f/mtags reload &7- reload config and tags.yml");
        core.messages().send(sender, "&f/mtags test &7- run diagnostics");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return match(args[0], List.of("add", "delete", "grant", "revoke", "clear", "list", "reload", "test", "help"));
        if ((args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("revoke") || args[0].equalsIgnoreCase("clear"))
                && args.length == 2) {
            return match(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
            return match(args[1], registry.ids());
        }
        if ((args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("revoke")) && args.length == 3) {
            return match(args[2], registry.ids());
        }
        return List.of();
    }

    private List<String> match(String input, Collection<String> options) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(option);
        }
        return result;
    }
}
