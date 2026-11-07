package com.theflai1k;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class Notify {
    private Notify() {}

    static final class T { String title, msg; int col; long t0; }
    static final List<T> L = new ArrayList<>();

    public static void push(String title, String msg, int col) {
        if (Modules.NOTIF != null && !Modules.NOTIF.enabled) return;
        T t = new T();
        t.title = title; t.msg = msg; t.col = col; t.t0 = System.currentTimeMillis();
        L.add(t);
        if (L.size() > 5) L.remove(0);
    }

    public static void render(DrawContext c) {
        if (L.isEmpty()) return;
        final long now = System.currentTimeMillis();
        final long life = (long) ((Modules.NOTIF == null ? 4 : Modules.NOTIF.s("Notification time (s)").value) * 1000);
        L.removeIf(t -> now - t.t0 > life);
        int sw = c.getScaledWindowWidth();
        int y = 6;
        for (T t : L) {
            long age = now - t.t0;
            int w = Math.max(Gfx.w(t.title), Gfx.w(t.msg)) + 18;
            float in = Math.min(1f, age / 220f);
            float out = Math.min(1f, (life - age) / 300f);
            float f = Math.max(0f, Math.min(in, out));
            int x = sw - w - 6 + (int) ((1 - in) * (w + 10));
            Gfx.rrect(c, x, y, w, 26, 5, Gfx.a(0xE00C0E12, f));
            c.fill(x + 3, y + 4, x + 5, y + 22, Gfx.a(t.col, f));
            Gfx.shadow(c, t.title, x + 10, y + 4, Gfx.a(0xFFFFFFFF, f));
            Gfx.text(c, t.msg, x + 10, y + 14, Gfx.a(0xFF9AA3B2, f));
            y += 30;
        }
    }
}
