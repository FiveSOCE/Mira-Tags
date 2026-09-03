package com.mira.tags;

import com.mira.core.api.MiraCore;
import com.mira.core.api.MiraCoreProvider;
import com.mira.core.api.ModuleHealth;
import com.mira.tags.api.MiraTagsApi;
import com.mira.tags.command.MiraTagsAdminCommand;
import com.mira.tags.command.TagsCommand;
import com.mira.tags.command.TimedTagCommand;
import com.mira.tags.gui.TagMenuService;
import com.mira.tags.listener.PlayerTagListener;
import com.mira.tags.listener.TagCreationListener;
import com.mira.tags.listener.TagMenuListener;
import com.mira.tags.service.LuckPermsTagService;
import com.mira.tags.service.MilestoneTagService;
import com.mira.tags.service.PlayerTagDataService;
import com.mira.tags.service.TagCreationService;
import com.mira.tags.service.TagRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MiraTagsPlugin extends JavaPlugin {
    private MiraCore core;
    private TagRegistry registry;
    private PlayerTagDataService playerData;
    private LuckPermsTagService tagService;
    private TagMenuService menus;
    private TagCreationService creation;
    private MilestoneTagService milestones;
    private MiraTagsApi api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "tags.yml").exists()) saveResource("tags.yml", false);

        core = MiraCoreProvider.require();
        registry = new TagRegistry(this);
        playerData = new PlayerTagDataService(this);
        tagService = new LuckPermsTagService(this, registry, playerData);
        menus = new TagMenuService(this, registry, playerData, tagService);
        creation = new TagCreationService(this, core, registry);
        milestones = new MilestoneTagService(this, core, registry, playerData, tagService);
        api = new MiraTagsApiImpl(this, registry, playerData, tagService);

        core.modules().register(this, "MiraTags");
        core.services().register(MiraTagsApi.class, api);

        getServer().getPluginManager().registerEvents(new TagMenuListener(this, menus, registry, playerData, tagService), this);
        getServer().getPluginManager().registerEvents(new PlayerTagListener(this, tagService), this);
        getServer().getPluginManager().registerEvents(new TagCreationListener(creation), this);

        PluginCommand tagsCommand = getCommand("tags");
        PluginCommand adminCommand = getCommand("miratags");
        PluginCommand timedCommand = getCommand("mtagtime");
        if (tagsCommand == null || adminCommand == null || timedCommand == null) {
            core.modules().setHealth(this, ModuleHealth.UNHEALTHY, "MiraTags commands missing from plugin.yml");
            throw new IllegalStateException("MiraTags commands missing from plugin.yml");
        }

        tagsCommand.setExecutor(new TagsCommand(menus));
        MiraTagsAdminCommand admin = new MiraTagsAdminCommand(this, core, registry, playerData, tagService, creation);
        adminCommand.setExecutor(admin);
        adminCommand.setTabCompleter(admin);
        timedCommand.setExecutor(new TimedTagCommand(core, milestones));

        getServer().getOnlinePlayers().forEach(player -> {
            milestones.sync(player);
            tagService.applyActive(player);
        });
        milestones.start();

        core.modules().setHealth(this, ModuleHealth.HEALTHY,
                "Tags, milestone achievements, seasonal tags and timed ownership ready");
        getLogger().info("MiraTags v" + getPluginMeta().getVersion() + " enabled with " + registry.enabledTags().size() + " enabled tag(s).");
    }

    @Override
    public void onDisable() {
        if (tagService != null) tagService.shutdown();
        if (playerData != null) playerData.save();
        if (core != null) {
            if (api != null) core.services().unregister(MiraTagsApi.class, api);
            core.modules().unregister(this);
        }
    }

    public void reloadPluginConfiguration() {
        reloadConfig();
        registry.reload();
        playerData.reload();
        tagService.refreshAll();
    }

    public int suffixPriority() { return Math.max(0, getConfig().getInt("luckperms.suffix-priority", 500)); }
    public String guiTitle() { return getConfig().getString("gui.title", "&5Tags"); }
    public boolean hideLockedTags() { return getConfig().getBoolean("gui.hide-locked", false); }
    public String lockedMessage() { return getConfig().getString("gui.locked-message", "&cYou have not unlocked this tag."); }
    public String equippedMessage() { return getConfig().getString("gui.equipped-message", "&aEquipped tag: &f%tag%"); }
    public String clearedMessage() { return getConfig().getString("gui.cleared-message", "&eYour active tag has been cleared."); }
}
