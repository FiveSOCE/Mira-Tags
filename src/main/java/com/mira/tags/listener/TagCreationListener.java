package com.mira.tags.listener;

import com.mira.tags.service.TagCreationService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class TagCreationListener implements Listener {
    private final TagCreationService creation;

    public TagCreationListener(TagCreationService creation) {
        this.creation = creation;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!creation.awaiting(event.getPlayer())) return;

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());
        creation.handleChat(event.getPlayer(), input);
    }
}
