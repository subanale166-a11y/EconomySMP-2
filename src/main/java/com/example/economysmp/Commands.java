package com.example.economysmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class Commands implements CommandExecutor {
    private final EconomySMP plugin;

    public Commands(EconomySMP plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender to, String s) {
        to.sendMessage(Util.color(plugin.getConfig().getString("messages.prefix", "") + s));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "balance" -> balance(sender, args);
            case "pay" -> pay(sender, args);
            case "sell" -> {
                if (sender instanceof Player p) plugin.sell().open(p);
                else msg(sender, "Players only.");
            }
            case "shop" -> {
                if (sender instanceof Player p) plugin.shop().open(p);
                else msg(sender, "Players only.");
            }
            case "shardshop" -> {
                if (sender instanceof Player p) plugin.shardShop().open(p);
                else msg(sender, "Players only.");
            }
            case "scoreboard" -> scoreboardCmd(sender, args);
            case "eco" -> eco(sender, args);
        }
        return true;
    }

    private void balance(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { msg(sender, "&cPlayer not found."); return; }
        } else if (sender instanceof Player p) {
            target = p;
        } else { msg(sender, "Usage: /balance <player>"); return; }

        PlayerData d = plugin.data().get(target.getUniqueId());
        msg(sender, "&f" + target.getName() + "&7: &a$" + Util.money(d.money)
                + " &7| &d" + d.shards + " shards");
    }

    private void pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { msg(sender, "Players only."); return; }
        if (args.length < 2) { msg(sender, "&cUsage: /pay <player> <amount>"); return; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { msg(sender, "&cPlayer not found."); return; }
        if (target.equals(p)) { msg(sender, "&cYou can't pay yourself."); return; }
        double amount = Util.parseAmount(args[1]);
        if (amount <= 0) { msg(sender, "&cInvalid amount."); return; }

        PlayerData from = plugin.data().get(p.getUniqueId());
        if (from.money < amount) { msg(sender, "&cYou don't have enough money."); return; }
        from.money -= amount;
        plugin.data().get(target.getUniqueId()).money += amount;
        plugin.sidebar().update(p);
        plugin.sidebar().update(target);
        msg(p, "Paid &f" + target.getName() + " &a$" + Util.money(amount) + "&7.");
        msg(target, "&f" + p.getName() + " &7paid you &a$" + Util.money(amount) + "&7.");
    }

    private String join(String[] a, int from) {
        return a.length <= from ? "" : String.join(" ", Arrays.copyOfRange(a, from, a.length));
    }

    private void scoreboardCmd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economysmp.admin")) { msg(sender, "&cNo permission."); return; }
        SidebarManager sb = plugin.sidebar();
        String sub = args.length > 0 ? args[0].toLowerCase() : "";
        switch (sub) {
            case "list" -> {
                msg(sender, "Title: &r" + sb.title());
                int n = 1;
                for (String line : sb.lines()) msg(sender, "&f" + n++ + ". &r" + (line.isEmpty() ? "&8(blank)" : line));
            }
            case "title" -> {
                String text = join(args, 1);
                if (text.isEmpty()) { msg(sender, "&cUsage: /scoreboard title <text>"); return; }
                sb.setTitle(text);
                msg(sender, "Title updated.");
            }
            case "add" -> {
                if (!sb.addLine(join(args, 1))) { msg(sender, "&cMax " + SidebarManager.MAX_LINES + " lines."); return; }
                msg(sender, "Line added.");
            }
            case "set" -> {
                int n = args.length > 1 ? parseInt(args[1]) : -1;
                if (n < 1 || !sb.setLine(n - 1, join(args, 2))) {
                    msg(sender, "&cUsage: /scoreboard set <line number> <text>");
                    return;
                }
                msg(sender, "Line " + n + " updated.");
            }
            case "remove" -> {
                int n = args.length > 1 ? parseInt(args[1]) : -1;
                if (n < 1 || !sb.removeLine(n - 1)) {
                    msg(sender, "&cUsage: /scoreboard remove <line number>");
                    return;
                }
                msg(sender, "Line " + n + " removed.");
            }
            default -> msg(sender, "&cUsage: /scoreboard <list|title|add|set|remove>");
        }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }

    private void eco(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economysmp.admin")) { msg(sender, "&cNo permission."); return; }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            msg(sender, "Reloaded config, shops and scoreboard.");
            return;
        }
        if (args.length < 4) {
            msg(sender, "&cUsage: /eco <give|take|set> <money|shards> <player> <amount>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { msg(sender, "&cPlayer not found."); return; }
        double amount = Util.parseAmount(args[3]);
        if (amount < 0) { msg(sender, "&cInvalid amount."); return; }

        PlayerData d = plugin.data().get(target.getUniqueId());
        boolean shards = args[1].equalsIgnoreCase("shards");
        if (!shards && !args[1].equalsIgnoreCase("money")) { msg(sender, "&cType must be money or shards."); return; }

        switch (args[0].toLowerCase()) {
            case "give" -> { if (shards) d.shards += (long) amount; else d.money += amount; }
            case "take" -> {
                if (shards) d.shards = Math.max(0, d.shards - (long) amount);
                else d.money = Math.max(0, d.money - amount);
            }
            case "set" -> { if (shards) d.shards = (long) amount; else d.money = amount; }
            default -> { msg(sender, "&cUse give, take or set."); return; }
        }
        plugin.sidebar().update(target);
        msg(sender, "Done. " + target.getName() + " now has &a$" + Util.money(d.money)
                + " &7and &d" + d.shards + " shards&7.");
    }
}
