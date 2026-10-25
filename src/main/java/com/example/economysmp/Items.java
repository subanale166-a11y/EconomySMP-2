package com.example.economysmp;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

public final class Items {
    private Items() {}

    /** Looks up an enchantment by name, e.g. "sharpness", "fire_aspect". */
    public static Enchantment find(String name) {
        try {
            Object o = Enchantment.class.getField(name.trim().toUpperCase()).get(null);
            return o instanceof Enchantment e ? e : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    /** Builds an item with enchants given as "name:level" strings. */
    public static ItemStack build(Material m, int amount, List<String> enchants, Logger log) {
        ItemStack stack = new ItemStack(m, amount);
        for (String s : enchants) {
            String[] parts = s.split(":");
            Enchantment e = find(parts[0]);
            int level = 1;
            if (parts.length > 1) {
                try { level = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) {}
            }
            if (e == null) {
                log.warning("Unknown enchantment: " + s);
                continue;
            }
            stack.addUnsafeEnchantment(e, level);
        }
        return stack;
    }
}
