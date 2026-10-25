package com.example.economysmp;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sidebar scoreboard (right-middle of the screen). Settings live in scoreboard.yml. */
public class SidebarManager {
    public static final int MAX_LINES = 15;

    private final EconomySMP plugin;
    private final File file;
    private YamlConfiguration yaml;
    private String title = "&b&lSMP";
    private List<String> lines = new ArrayList<>();
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public SidebarManager(EconomySMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "scoreboard.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) plugin.saveResource("scoreboard.yml", false);
        yaml = YamlConfiguration.loadConfiguration(file);
        title = yaml.getString("title", "&b&lSMP");
        lines = new ArrayList<>(yaml.getStringList("lines"));
        if (lines.size() > MAX_LINES) lines = new ArrayList<>(lines.subList(0, MAX_LINES));
        boards.clear(); // boards are rebuilt on the next update
    }

    private void save() {
        yaml.set("title", title);
        yaml.set("lines", lines);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save scoreboard.yml: " + e.getMessage());
        }
        boards.clear();
    }

    // ----- editing (used by /scoreboard) -----
    public String title() { return title; }
    public List<String> lines() { return new ArrayList<>(lines); }

    public void setTitle(String t) { title = t; save(); }
    public boolean addLine(String text) {
        if (lines.size() >= MAX_LINES) return false;
        lines.add(text);
        save();
        return true;
    }
    public boolean setLine(int index, String text) {
        if (index < 0 || index >= lines.size()) return false;
        lines.set(index, text);
        save();
        return true;
    }
    public boolean removeLine(int index) {
        if (index < 0 || index >= lines.size()) return false;
        lines.remove(index);
        save();
        return true;
    }

    // ----- display -----
    public void remove(Player p) {
        boards.remove(p.getUniqueId());
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void update(Player p) {
        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null) {
            sb = create();
            boards.put(p.getUniqueId(), sb);
        }

        PlayerData d = plugin.data().get(p.getUniqueId());
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i)
                    .replace("{money}", Util.money(d.money))
                    .replace("{shards}", String.valueOf(d.shards))
                    .replace("{playtime}", Util.time(d.playtime))
                    .replace("{player}", p.getName());
            Team t = sb.getTeam("line" + i);
            if (t != null) t.prefix(Util.color(text));
        }

        if (p.getScoreboard() != sb) p.setScoreboard(sb);
    }

    private Scoreboard create() {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("sidebar", Criteria.DUMMY, Util.color(title));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank()); // hide the red score numbers

        for (int i = 0; i < lines.size(); i++) {
            String entry = "\u00a7" + Integer.toHexString(i); // unique invisible entry
            Team t = sb.registerNewTeam("line" + i);
            t.addEntry(entry);
            obj.getScore(entry).setScore(lines.size() - i);
        }
        return sb;
    }
}
