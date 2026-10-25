package com.example.economysmp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomySMP extends JavaPlugin {
    private DataManager data;
    private SidebarManager sidebar;
    private SellManager sell;
    private ShardShop shardShop;
    private ShopManager shop;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataManager(this);
        sidebar = new SidebarManager(this);
        sell = new SellManager(this);
        shardShop = new ShardShop(this);
        shop = new ShopManager(this);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(sell, this);
        Bukkit.getPluginManager().registerEvents(shardShop, this);
        Bukkit.getPluginManager().registerEvents(shop, this);

        Commands commands = new Commands(this);
        for (String name : new String[]{"balance", "pay", "sell", "shop", "shardshop", "scoreboard", "eco"}) {
            getCommand(name).setExecutor(commands);
        }

        long interval = Math.max(1, getConfig().getLong("shards.seconds-per-shard", 600));
        long amount = getConfig().getLong("shards.amount", 1);

        // Runs every second: playtime, shard rewards, sidebar refresh
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData d = data.get(p.getUniqueId());
                d.playtime++;
                if (d.playtime % interval == 0) {
                    d.shards += amount;
                    p.sendMessage(Util.color(getConfig().getString("messages.prefix", "")
                            + "You earned &d" + amount + " shard(s) &7for playing!"));
                }
                sidebar.update(p);
            }
        }, 20L, 20L);

        // Autosave every 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> data.saveAll(), 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (data != null) data.saveAll();
    }

    public void reloadAll() {
        reloadConfig();
        sell.reload();
        shop.reload();
        shardShop.reload();
        sidebar.reload();
    }

    public DataManager data() { return data; }
    public SidebarManager sidebar() { return sidebar; }
    public SellManager sell() { return sell; }
    public ShardShop shardShop() { return shardShop; }
    public ShopManager shop() { return shop; }
}
