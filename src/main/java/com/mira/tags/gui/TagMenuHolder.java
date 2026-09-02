package com.mira.tags.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class TagMenuHolder implements InventoryHolder {
    private final int page;
    private final Map<Integer, String> slots = new HashMap<>();
    private Inventory inventory;

    public TagMenuHolder(int page) {
        this.page = page;
    }

    public int page() {
        return page;
    }

    public void bind(int slot, String tagId) {
        slots.put(slot, tagId);
    }

    public Optional<String> tagAt(int slot) {
        return Optional.ofNullable(slots.get(slot));
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
