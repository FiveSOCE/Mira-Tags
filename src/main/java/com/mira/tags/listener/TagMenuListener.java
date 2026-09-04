package com.mira.tags.listener;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.gui.TagMenuHolder;
import com.mira.tags.gui.TagMenuService;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.service.LuckPermsTagService;
import com.mira.tags.service.PlayerTagDataService;
import com.mira.tags.service.TagRegistry;
import com.mira.tags.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Optional;

public final class TagMenuListener implements Listener {
    private static final String PREFIX = "&5&lMira &8&l>> &r";
    private final MiraTagsPlugin plugin;
    private final TagMenuService menus;
    private final TagRegistry registry;
    private final PlayerTagDataService playerData;
    private final LuckPermsTagService tagService;

    public TagMenuListener(MiraTagsPlugin plugin, TagMenuService menus, TagRegistry registry,
                           PlayerTagDataService playerData, LuckPermsTagService tagService) {
        this.plugin = plugin;
        this.menus = menus;
        this.registry = registry;
        this.playerData = playerData;
        this.tagService = tagService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TagMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) return;

        if (rawSlot == TagMenuService.PREVIOUS_SLOT) { menus.open(player, holder.page() - 1); return; }
        if (rawSlot == TagMenuService.NEXT_SLOT) { menus.open(player, holder.page() + 1); return; }
        if (rawSlot == TagMenuService.CLEAR_SLOT) {
            tagService.clear(player);
            player.sendMessage(Text.component(PREFIX + plugin.clearedMessage()));
            menus.open(player, holder.page());
            return;
        }

        Optional<String> tagId = holder.tagAt(rawSlot);
        if (tagId.isEmpty()) return;
        Optional<TagDefinition> tag = registry.find(tagId.get());
        if (tag.isEmpty()) { menus.open(player, holder.page()); return; }

        if (!tagService.owns(player, tag.get())) {
            player.sendMessage(Text.component(PREFIX + plugin.lockedMessage()));
            return;
        }

        boolean alreadyActive = playerData.active(player.getUniqueId()).filter(tag.get().id()::equals).isPresent();
        if (alreadyActive) {
            tagService.clear(player);
            player.sendMessage(Text.component(PREFIX + plugin.clearedMessage()));
        } else if (tagService.equip(player, tag.get().id())) {
            player.sendMessage(Text.component(PREFIX + plugin.equippedMessage().replace("%tag%", tag.get().displayName())));
        }
        menus.open(player, holder.page());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof TagMenuHolder)) return;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < event.getInventory().getSize()) { event.setCancelled(true); return; }
        }
    }
}
