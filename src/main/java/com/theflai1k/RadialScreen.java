package com.theflai1k;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RadialScreen extends Screen {
    final List<Mod> items = new ArrayList<>();
    final int openKey;

    public RadialScreen(int openKey) {
        super(Text.literal("Radial menu"));
        this.openKey = openKey;
        for (Mod m : Modules.ALL) if (m.pinned && !m.isTool() && items.size() < 10) items.add(m);
    }

    @Override public boolean shouldPause() { return false; }

    int[] pos(int i, int cx, int cy) {
        double R = items.size() > 8 ? 90 : 72;
        double ang = Math.toRadians(-90 + 360.0 * i / Math.max(1, items.size()));
        return new int[]{(int) (cx + Math.cos(ang) * R), (int) (cy + Math.sin(ang) * R)};
    }

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        int cx = width / 2, cy = height / 2;
        Gfx.rrect(c, cx - 30, cy - 12, 60, 24, 12, 0xD00C0E12);
        String t = "TheFlai1k";
        Gfx.text(c, t, cx - Gfx.w(t) / 2, cy - 4, 0xFFF2F4F8);
        if (items.isEmpty()) {
            String s = "Pin modules in their settings (Pin to Radial Menu)";
            Gfx.shadow(c, s, cx - Gfx.w(s) / 2, cy + 24, 0xFF9AA3B2);
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            Mod m = items.get(i);
            int[] p = pos(i, cx, cy);
            int x = p[0] - 34, y = p[1] - 10;
            boolean hov = mx >= x && mx < x + 68 && my >= y && my < y + 20;
            Gfx.rrect(c, x - 1, y - 1, 70, 22, 7, m.enabled ? Gfx.a(Config.accent(), 0.6f) : 0x30FFFFFF);
            Gfx.rrect(c, x, y, 68, 20, 6, hov ? 0xE0232833 : 0xD0181B21);
            String n = Gfx.fit(m.name, 62);
            Gfx.text(c, n, p[0] - Gfx.w(n) / 2, y + 6, m.enabled ? Config.accent() : 0xFFF2F4F8);
        }
    }

    @Override public boolean mouseClicked(double dx, double dy, int button) {
        int cx = width / 2, cy = height / 2;
        for (int i = 0; i < items.size(); i++) {
            int[] p = pos(i, cx, cy);
            if (dx >= p[0] - 34 && dx < p[0] + 34 && dy >= p[1] - 10 && dy < p[1] + 10) {
                Mod m = items.get(i);
                m.setEnabled(!m.enabled);
                return true;
            }
        }
        return super.mouseClicked(dx, dy, button);
    }

    @Override public boolean keyPressed(int key, int sc, int mods) {
        if (key == openKey) { close(); return true; }
        return super.keyPressed(key, sc, mods);
    }

    @Override public void close() { Config.save(); super.close(); }
}
