package com.mira.tags.command;

import com.mira.tags.gui.TagMenuService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TagsCommand implements CommandExecutor {
    private final TagMenuService menus;

    public TagsCommand(TagMenuService menus) {
        this.menus = menus;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }
        menus.open(player);
        return true;
    }
}
