package com.theflai1k;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Mod {
    public enum Cat {
        HUD("HUD"), COMBAT("Combat"), MOVEMENT("Movement"), VISUAL("Visual"), WORLD("World"), UTILITY("Utility");
        public final String label;
        Cat(String l) { label = l; }
    }

    public interface Painter { void paint(Mod m, DrawContext c, int x, int y); }

    public final String name, desc;
    public final Cat cat;
    public boolean enabled, hold, hud, full, active, wasDown, b1, b2, pinned;
    public int key = -1, x, y, defX, defY, w = 60, h = 14, i1, tab = -1, page;
    public long t1;
    public double d1;
    public float anim;
    public final List<Setting> settings = new ArrayList<>();
    public final List<Object[]> actions = new ArrayList<>();
    public BiConsumer<Mod, MinecraftClient> onTick, onKey;
    public Consumer<Mod> onEnable, onDisable;
    public Function<Mod, List<String>> lines;
    public Painter painter;

    public Mod(String name, String desc, Cat cat) { this.name = name; this.desc = desc; this.cat = cat; }

    public Mod hud(int x, int y) { hud = true; this.x = x; this.y = y; defX = x; defY = y; return this; }
    public Mod center(int y) { return hud(-1, y); }
    public Mod full() { hud = true; full = true; return this; }
    public Mod on() { enabled = true; return this; }
    public Mod defKey(int k) { key = k; return this; }
    public Mod holdKey(int k) { hold = true; key = k; return this; }
    public Mod pin() { pinned = true; return this; }
    public Mod with(Setting s) { settings.add(s); return this; }
    public Mod lines(Function<Mod, List<String>> f) { lines = f; return this; }
    public Mod paint(Painter p) { painter = p; return this; }
    public Mod tick(BiConsumer<Mod, MinecraftClient> t) { onTick = t; return this; }
    public Mod keyAct(BiConsumer<Mod, MinecraftClient> t) { onKey = t; return this; }
    public Mod enable(Consumer<Mod> r) { onEnable = r; return this; }
    public Mod disable(Consumer<Mod> r) { onDisable = r; return this; }
    public Mod opens(int tab, int page) { this.tab = tab; this.page = page; return this; }
    public Mod act(String label, Consumer<MinecraftClient> a) { actions.add(new Object[]{label, a}); return this; }

    public boolean isTool() { return tab >= 0; }

    public Setting s(String n) {
        for (Setting st : settings) if (st.name.equals(n)) return st;
        throw new IllegalArgumentException("No setting " + n + " in " + name);
    }

    public void setEnabled(boolean v) {
        if (v == enabled) return;
        enabled = v;
        if (v && onEnable != null) onEnable.accept(this);
        if (!v && onDisable != null) onDisable.accept(this);
    }

    public void press(MinecraftClient mc) {
        if (isTool()) { mc.setScreen(new MenuScreen(tab, page)); return; }
        if (onKey != null) { if (enabled) onKey.accept(this, mc); return; }
        setEnabled(!enabled);
        if (mc.player != null) mc.player.sendMessage(Text.literal("\u00a77[\u00a7fTheFlai1k\u00a77] \u00a7f" + name + " " + (enabled ? "\u00a7aON" : "\u00a7cOFF")), true);
    }

    public void paint(DrawContext c, int px, int py) {
        if (lines != null) Hud.panel(c, this, px, py, lines.apply(this));
        else if (painter != null) painter.paint(this, c, px, py);
    }
}
