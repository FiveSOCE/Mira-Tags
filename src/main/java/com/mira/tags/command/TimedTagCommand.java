package com.mira.tags.command;

import com.mira.core.api.MiraCore;
import com.mira.tags.service.MilestoneTagService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class TimedTagCommand implements CommandExecutor {
    private final MiraCore core;
    private final MilestoneTagService milestones;

    public TimedTagCommand(MiraCore core, MilestoneTagService milestones) {
        this.core = core;
        this.milestones = milestones;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 3) return false;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(core.messages().parse("&cThat player must be online."));
            return true;
        }
        Duration duration = parse(args[2]);
        if (duration == null) {
            sender.sendMessage(core.messages().parse("&cUse a duration like 30m, 12h or 7d."));
            return true;
        }
        if (!milestones.grantTimed(target, args[1], duration)) {
            sender.sendMessage(core.messages().parse("&cUnknown tag or the player already owns it."));
            return true;
        }
        sender.sendMessage(core.messages().parse("&aGranted &f" + args[1] + " &ato &f" + target.getName() + " &afor &f" + args[2] + "&a."));
        return true;
    }

    private Duration parse(String raw) {
        try {
            if (raw.length() < 2) return null;
            long value = Long.parseLong(raw.substring(0, raw.length() - 1));
            return switch (Character.toLowerCase(raw.charAt(raw.length() - 1))) {
                case 'm' -> Duration.ofMinutes(value);
                case 'h' -> Duration.ofHours(value);
                case 'd' -> Duration.ofDays(value);
                default -> null;
            };
        } catch (Exception ignored) { return null; }
    }
}
