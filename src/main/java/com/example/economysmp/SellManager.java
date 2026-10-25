package com.example.economysmp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.EnumMap;
import java.util.Map;

public class SellManager implements Listener {
    private final EconomySMP plugin;
    private final Map<Material, Double> prices = new EnumMap<>(Material.class);
    private double defaultPrice = 1.0; // price per item for anything not on the list (0 = disabled)

    public SellManager(EconomySMP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        prices.clear();
        defaultPrice = plugin.getConfig().getDouble("sell.default-price", 1.0);
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("sell.prices");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            Material m = Material.matchMaterial(key);
            if (m == null) {
                plugin.getLogger().warning("Unknown material in sell.prices: " + key);
                continue;
            }
            prices.put(m, sec.getDouble(key));
        }
    }

    public static class SellHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
        void set(Inventory i) { this.inv = i; }
    }

    public void open(Player p) {
        SellHolder holder = new SellHolder();
        Inventory inv = Bukkit.createInventory(holder, 54,
                Util.color(plugin.getConfig().getString("sell.title", "&8Sell Items")));
        holder.set(inv);
        p.openInventory(inv);
    }

    private boolean holdsItems(ItemStack it) {
        return it.getItemMeta() instanceof BlockStateMeta meta
                && meta.getBlockState() instanceof ShulkerBox box
                && !box.getInventory().isEmpty();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof SellHolder)) return;
        Player p = (Player) e.getPlayer();

        double total = 0;
        int sold = 0;
        for (ItemStack it : e.getInventory().getContents()) {
            if (it == null || it.getType().isAir()) continue;
            Double price = prices.get(it.getType());
            if (price == null && defaultPrice > 0) price = defaultPrice;
            if (price != null && holdsItems(it)) price = null; // never destroy a filled shulker box
            if (price == null) {
                // not sellable -> give it back
                p.getInventory().addItem(it).values()
                        .forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
            } else {
                total += price * it.getAmount();
                sold += it.getAmount();
            }
        }
        e.getInventory().clear();

        String prefix = plugin.getConfig().getString("messages.prefix", "");
        if (sold > 0) {
            plugin.data().get(p.getUniqueId()).money += total;
            plugin.sidebar().update(p);
            p.sendMessage(Util.color(prefix + "Sold &f" + sold + " &7items for &a$" + Util.money(total) + "&7."));
        } else {
            p.sendMessage(Util.color(prefix + "Nothing was sold."));
        }
    }
}
