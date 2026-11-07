package com.theflai1k;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Config {
    private Config() {}

    public static final int[] ACC = {0xFFFFFFFF, 0xFF7AA2FF, 0xFFB07CFF, 0xFFFF7AB6, 0xFF6EE7A0, 0xFFFFA05A};
    public static int accentIdx = 0;
    public static final Setting OPACITY = new Setting("HUD opacity", 1, 0.2, 1, 0.05);
    public static String notes = "";
    public static final List<String> friends = new ArrayList<>();
    public static final List<Waypoint> waypoints = new ArrayList<>();
    public static final Map<String, List<String>> palettes = new LinkedHashMap<>();
    public static final Map<String, List<String>> layouts = new LinkedHashMap<>();

    public static class Waypoint {
        public String name = "", dim = "overworld";
        public int x, y, z;
        public long t;
    }

    public static int accent() { return ACC[Math.max(0, Math.min(ACC.length - 1, accentIdx))]; }
    public static float opacity() { return OPACITY.f(); }

    static class Data {
        Map<String, Boolean> en = new HashMap<>();
        Map<String, Boolean> pins = new HashMap<>();
        Map<String, Integer> keys = new HashMap<>();
        Map<String, Double> vals = new HashMap<>();
        Map<String, int[]> pos = new HashMap<>();
        float opacity = 1f;
        int accent = 0;
        String notes = "";
        List<String> friends = new ArrayList<>();
        List<Waypoint> waypoints = new ArrayList<>();
        Map<String, List<String>> palettes = new LinkedHashMap<>();
        Map<String, List<String>> layouts = new LinkedHashMap<>();
    }

    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("theflai1k.json"); }

    public static void save() {
        try {
            Data d = new Data();
            for (Mod m : Modules.ALL) {
                d.en.put(m.name, m.enabled);
                d.pins.put(m.name, m.pinned);
                d.keys.put(m.name, m.key);
                for (Setting s : m.settings) d.vals.put(m.name + "." + s.name, s.value);
                if (m.hud && !m.full) d.pos.put(m.name, new int[]{m.x, m.y});
            }
            d.opacity = OPACITY.f();
            d.accent = accentIdx;
            d.notes = notes;
            d.friends = new ArrayList<>(friends);
            d.waypoints = new ArrayList<>(waypoints);
            d.palettes = new LinkedHashMap<>(palettes);
            d.layouts = new LinkedHashMap<>(layouts);
            Files.writeString(file(), GSON.toJson(d));
        } catch (Exception ignored) {}
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Data d = GSON.fromJson(Files.readString(f), Data.class);
            if (d == null) return;
            for (Mod m : Modules.ALL) {
                if (d.en != null && d.en.containsKey(m.name)) m.enabled = d.en.get(m.name);
                if (d.pins != null && d.pins.containsKey(m.name)) m.pinned = d.pins.get(m.name);
                if (d.keys != null && d.keys.containsKey(m.name)) m.key = d.keys.get(m.name);
                for (Setting s : m.settings) {
                    if (d.vals != null && d.vals.containsKey(m.name + "." + s.name)) s.value = d.vals.get(m.name + "." + s.name);
                }
                if (d.pos != null && d.pos.containsKey(m.name)) { int[] p = d.pos.get(m.name); if (p.length == 2) { m.x = p[0]; m.y = p[1]; } }
            }
            OPACITY.value = d.opacity <= 0 ? 1 : d.opacity;
            accentIdx = d.accent;
            notes = d.notes == null ? "" : d.notes;
            friends.clear(); if (d.friends != null) friends.addAll(d.friends);
            waypoints.clear(); if (d.waypoints != null) waypoints.addAll(d.waypoints);
            palettes.clear(); if (d.palettes != null) palettes.putAll(d.palettes);
            layouts.clear(); if (d.layouts != null) layouts.putAll(d.layouts);
        } catch (Exception ignored) {}
    }
}
