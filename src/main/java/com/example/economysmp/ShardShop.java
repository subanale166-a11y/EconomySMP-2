package com.example.economysmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShardShop implements Listener {
    private final EconomySMP plugin;
    private final Map<Integer, Entry> entries = new HashMap<>();
    private String title = "&5Shard Shop";
    private int size = 54;

    private record Entry(Material material, long cost, List<String> enchants) {}

    public ShardShop(EconomySMP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        entries.clear();
        File file = new File(plugin.getDataFolder(), "shardshop.yml");
        if (!file.exists()) plugin.saveResource("shardshop.yml", false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        title = yaml.getString("title", "&5Shard Shop");
        size = Math.max(9, Math.min(54, (yaml.getInt("size", 54) / 9) * 9));

        ConfigurationSection sec = yaml.getConfigurationSection("items");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Material m = Material.matchMaterial(s.getString("material", ""));
            if (m == null) {
                plugin.getLogger().warning("Unknown material in shardshop.yml: " + key);
                continue;
            }
            entries.put(s.getInt("slot"), new Entry(m, s.getLong("cost"), s.getStringList("enchants")));
        }
    }

    public static class ShopHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
        void set(Inventory i) { this.inv = i; }
    }

    public void open(Player p) {
        ShopHolder holder = new ShopHolder();
        Inventory inv = Bukkit.createInventory(holder, size, Util.color(title));
        holder.set(inv);

        entries.forEach((slot, entry) -> {
            if (slot < 0 || slot >= size) return;
            ItemStack item = Items.build(entry.material(), 1, entry.enchants(), plugin.getLogger());
            ItemMeta meta = item.getItemMeta();
            meta.lore(List.<Component>of(
                    Util.color("&7Cost: &d" + entry.cost() + " shards").decoration(TextDecoration.ITALIC, false),
                    Util.color("&eClick to buy").decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        });
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ShopHolder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        Entry entry = entries.get(e.getSlot());
        if (entry == null) return;

        Player p = (Player) e.getWhoClicked();
        PlayerData d = plugin.data().get(p.getUniqueId());
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (d.shards < entry.cost()) {
            p.sendMessage(Util.color(prefix + "&cYou need &d" + entry.cost()
                    + " shards&c, you only have &d" + d.shards + "&c."));
            return;
        }
        d.shards -= entry.cost();
        ItemStack bought = Items.build(entry.material(), 1, entry.enchants(), plugin.getLogger());
        p.getInventory().addItem(bought).values()
                .forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
        plugin.sidebar().update(p);
        p.sendMessage(Util.color(prefix + "Purchased for &d" + entry.cost() + " shards&7."));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ShopHolder) e.setCancelled(true);
    }
}
