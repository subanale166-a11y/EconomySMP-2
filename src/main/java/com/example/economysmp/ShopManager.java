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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** /shop - category menu where players buy items with money. */
public class ShopManager implements Listener {
    private static final int BACK_SLOT = 49;
    private static final int BALANCE_SLOT = 53;
    private static final int SHARD_BUTTON_SLOT = 22;

    private final EconomySMP plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private String mainTitle = "&8Shop";

    private record ShopItem(Material material, double price, List<String> enchants) {}

    private static class Category {
        String id;
        String name;
        Material icon;
        int slot;
        final List<ShopItem> items = new ArrayList<>();
    }

    public static class MenuHolder implements InventoryHolder {
        final String category; // null = main menu
        Inventory inv;
        MenuHolder(String category) { this.category = category; }
        @Override public Inventory getInventory() { return inv; }
    }

    public ShopManager(EconomySMP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        categories.clear();
        File file = new File(plugin.getDataFolder(), "buyshop.yml");
        if (!file.exists()) plugin.saveResource("buyshop.yml", false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        mainTitle = yaml.getString("title", "&8Shop");

        ConfigurationSection cats = yaml.getConfigurationSection("categories");
        if (cats == null) return;
        for (String id : cats.getKeys(false)) {
            ConfigurationSection s = cats.getConfigurationSection(id);
            if (s == null) continue;
            Category c = new Category();
            c.id = id;
            c.name = s.getString("name", id);
            c.icon = Material.matchMaterial(s.getString("icon", "CHEST"));
            if (c.icon == null) c.icon = Material.CHEST;
            c.slot = s.getInt("slot", 10);

            ConfigurationSection items = s.getConfigurationSection("items");
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    Material m;
                    double price;
                    List<String> enchants = List.of();
                    if (items.isConfigurationSection(key)) {
                        ConfigurationSection is = items.getConfigurationSection(key);
                        m = Material.matchMaterial(is.getString("material", key));
                        price = is.getDouble("price");
                        enchants = is.getStringList("enchants");
                    } else {
                        m = Material.matchMaterial(key);
                        price = items.getDouble(key);
                    }
                    if (m == null) {
                        plugin.getLogger().warning("Unknown material in buyshop.yml: " + key);
                        continue;
                    }
                    if (c.items.size() < 45) c.items.add(new ShopItem(m, price, enchants));
                }
            }
            categories.put(id, c);
        }
    }

    // ---------- helpers ----------

    private Component plain(String text) {
        return Util.color(text).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack item(Material m, String name, String... lore) {
        return decorate(new ItemStack(m), name, lore);
    }

    private ItemStack decorate(ItemStack stack, String name, String... lore) {
        ItemMeta meta = stack.getItemMeta();
        if (name != null) meta.displayName(plain(name));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) lines.add(plain(l));
            meta.lore(lines);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack balanceItem(Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        return item(Material.GOLD_INGOT, "&6Your Balance",
                "&a$" + Util.money(d.money), "&d" + d.shards + " shards");
    }

    private void fill(Inventory inv, int from, int to) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = from; i < to; i++) inv.setItem(i, filler);
    }

    // ---------- menus ----------

    public void open(Player p) {
        MenuHolder holder = new MenuHolder(null);
        Inventory inv = Bukkit.createInventory(holder, 27, Util.color(mainTitle));
        holder.inv = inv;
        fill(inv, 0, 27);

        for (Category c : categories.values()) {
            if (c.slot < 0 || c.slot >= 27) continue;
            inv.setItem(c.slot, item(c.icon, c.name, "&7" + c.items.size() + " items", "", "&eClick to browse"));
        }
        inv.setItem(4, balanceItem(p));
        inv.setItem(SHARD_BUTTON_SLOT, item(Material.NETHERITE_CHESTPLATE, "&5Shard Shop",
                "&7Spend shards on netherite armor", "", "&eClick to open"));
        p.openInventory(inv);
    }

    private void openCategory(Player p, Category c) {
        MenuHolder holder = new MenuHolder(c.id);
        Inventory inv = Bukkit.createInventory(holder, 54, Util.color(c.name));
        holder.inv = inv;
        fill(inv, 45, 54);

        for (int i = 0; i < c.items.size(); i++) {
            ShopItem si = c.items.get(i);
            ItemStack shown = Items.build(si.material(), 1, si.enchants(), plugin.getLogger());
            if (si.material().getMaxStackSize() == 1) {
                inv.setItem(i, decorate(shown, null,
                        "&7Price: &a$" + Util.money(si.price()),
                        "",
                        "&eClick to buy"));
            } else {
                inv.setItem(i, decorate(shown, null,
                        "&7Price: &a$" + Util.money(si.price()) + " &7each",
                        "",
                        "&eLeft-click: &fbuy 1",
                        "&eRight-click: &fbuy 16",
                        "&eShift-click: &fbuy a stack"));
            }
        }
        inv.setItem(BACK_SLOT, item(Material.ARROW, "&cBack"));
        inv.setItem(BALANCE_SLOT, balanceItem(p));
        p.openInventory(inv);
    }

    // ---------- events ----------

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        if (holder.category == null) {
            if (slot == SHARD_BUTTON_SLOT) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.shardShop().open(p));
                return;
            }
            for (Category c : categories.values()) {
                if (c.slot == slot) {
                    Bukkit.getScheduler().runTask(plugin, () -> openCategory(p, c));
                    return;
                }
            }
            return;
        }

        if (slot == BACK_SLOT) {
            Bukkit.getScheduler().runTask(plugin, () -> open(p));
            return;
        }
        Category c = categories.get(holder.category);
        if (c == null || slot < 0 || slot >= c.items.size()) return;

        ShopItem si = c.items.get(slot);
        int max = si.material().getMaxStackSize();
        int amount = e.isShiftClick() ? max : e.isRightClick() ? Math.min(16, max) : 1;
        buy(p, si, amount);
        e.getView().getTopInventory().setItem(BALANCE_SLOT, balanceItem(p));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MenuHolder) e.setCancelled(true);
    }

    private void buy(Player p, ShopItem si, int amount) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        PlayerData d = plugin.data().get(p.getUniqueId());
        double cost = si.price() * amount;

        if (d.money < cost) {
            p.sendMessage(Util.color(prefix + "&cYou need &a$" + Util.money(cost)
                    + "&c, you only have &a$" + Util.money(d.money) + "&c."));
            return;
        }
        d.money -= cost;
        p.getInventory().addItem(Items.build(si.material(), amount, si.enchants(), plugin.getLogger())).values()
                .forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
        plugin.sidebar().update(p);
        p.sendMessage(Util.color(prefix + "Bought &f" + amount + "x "
                + si.material().name().toLowerCase().replace('_', ' ')
                + " &7for &a$" + Util.money(cost) + "&7."));
    }
}
