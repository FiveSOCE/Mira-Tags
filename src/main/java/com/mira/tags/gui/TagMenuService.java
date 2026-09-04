package com.mira.tags.gui;

import com.mira.tags.MiraTagsPlugin;
import com.mira.tags.model.TagDefinition;
import com.mira.tags.service.LuckPermsTagService;
import com.mira.tags.service.PlayerTagDataService;
import com.mira.tags.service.TagRegistry;
import com.mira.tags.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TagMenuService {
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int CLEAR_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final MiraTagsPlugin plugin;
    private final TagRegistry registry;
    private final PlayerTagDataService playerData;
    private final LuckPermsTagService tagService;

    public TagMenuService(MiraTagsPlugin plugin, TagRegistry registry,
                          PlayerTagDataService playerData, LuckPermsTagService tagService) {
        this.plugin = plugin;
        this.registry = registry;
        this.playerData = playerData;
        this.tagService = tagService;
    }

    public void open(Player player, int requestedPage) {
        tagService.applyActive(player);

        List<TagDefinition> visible = registry.enabledTags().stream()
                .filter(tag -> !plugin.hideLockedTags() || tagService.owns(player, tag))
                .toList();
        int pages = Math.max(1, (int) Math.ceil(visible.size() / (double) PAGE_SIZE));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));

        TagMenuHolder holder = new TagMenuHolder(page);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Text.component(plugin.guiTitle() + " &8(" + (page + 1) + "/" + pages + ")"));
        holder.inventory(inventory);

        Optional<String> active = playerData.active(player.getUniqueId());
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, visible.size());
        int slot = 0;
        for (int index = from; index < to; index++) {
            TagDefinition tag = visible.get(index);
            boolean owned = tagService.owns(player, tag);
            boolean selected = active.filter(tag.id()::equals).isPresent();
            inventory.setItem(slot, tagItem(tag, owned, selected));
            holder.bind(slot, tag.id());
            slot++;
        }

        if (page > 0) inventory.setItem(PREVIOUS_SLOT, button(Material.ARROW, "&dPrevious Page", List.of("&7Go back one page.")));
        inventory.setItem(CLEAR_SLOT, button(Material.BARRIER, "&cClear Active Tag",
                List.of(active.isPresent() ? "&7Currently: &f" + active.get() : "&7No tag is currently equipped.")));
        if (page + 1 < pages) inventory.setItem(NEXT_SLOT, button(Material.ARROW, "&dNext Page", List.of("&7Go forward one page.")));

        player.openInventory(inventory);
    }

    public void open(Player player) {
        open(player, 0);
    }

    private ItemStack tagItem(TagDefinition tag, boolean owned, boolean selected) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(tag.suffix().stripLeading()));

        List<Component> lore = new ArrayList<>();
        for (String line : tag.description()) lore.add(Text.component(line));
        if (!tag.description().isEmpty()) lore.add(Component.empty());

        lore.add(Text.component("&8ID: &7" + tag.id()));
        lore.add(Text.component("&8Preview: &fYourName" + tag.suffix()));

        if (selected) {
            lore.add(Text.component("&aEquipped"));
            lore.add(Text.component("&7Click to clear this tag."));
        } else if (owned) {
            lore.add(Text.component("&aUnlocked"));
            lore.add(Text.component("&7Click to equip."));
        } else {
            lore.add(Text.component("&cLocked"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(name));
        meta.lore(loreLines.stream().map(Text::component).toList());
        item.setItemMeta(meta);
        return item;
    }
}
