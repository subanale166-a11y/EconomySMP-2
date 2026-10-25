package com.example.economysmp;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final EconomySMP plugin;

    public PlayerListener(EconomySMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.data().get(e.getPlayer().getUniqueId());
        plugin.sidebar().update(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.sidebar().remove(e.getPlayer());
        plugin.data().unload(e.getPlayer().getUniqueId());
    }
}
