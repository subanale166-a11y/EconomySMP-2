package com.example.economysmp;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final EconomySMP plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataManager(EconomySMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        plugin.getDataFolder().mkdirs();
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public PlayerData get(UUID id) {
        return cache.computeIfAbsent(id, k -> {
            String path = "players." + k;
            return new PlayerData(
                    yaml.getDouble(path + ".money", plugin.getConfig().getDouble("starting-balance", 0)),
                    yaml.getLong(path + ".shards", 0),
                    yaml.getLong(path + ".playtime", 0));
        });
    }

    public void unload(UUID id) {
        PlayerData d = cache.remove(id);
        if (d != null) write(id, d);
        flush();
    }

    public void saveAll() {
        cache.forEach(this::write);
        flush();
    }

    private void write(UUID id, PlayerData d) {
        String path = "players." + id;
        yaml.set(path + ".money", d.money);
        yaml.set(path + ".shards", d.shards);
        yaml.set(path + ".playtime", d.playtime);
    }

    private void flush() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save players.yml: " + e.getMessage());
        }
    }
}
