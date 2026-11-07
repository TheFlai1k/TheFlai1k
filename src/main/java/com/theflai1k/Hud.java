package com.theflai1k;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.List;

public final class Hud {
    private Hud() {}

    static final ArrayDeque<Long> LQ = new ArrayDeque<>(), RQ = new ArrayDeque<>();
    static boolean lastL, lastR, wasDown;
    static Mod dragging;
    static int offX, offY;

    public static int cps(int b) {
        ArrayDeque<Long> q = b == 0 ? LQ : RQ;
        long n = System.currentTimeMillis();
        while (!q.isEmpty() && n - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    static void poll(MinecraftClient mc) {
        if (mc.currentScreen != null) { lastL = lastR = false; return; }
        long h = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, 0) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, 1) == GLFW.GLFW_PRESS;
        if (l && !lastL) LQ.add(System.currentTimeMillis());
        if (r && !lastR) RQ.add(System.currentTimeMillis());
        lastL = l; lastR = r;
    }

    public static int bg() { return (((int) (0xB4 * Config.opacity())) << 24) | 0x0E1014; }

    public static boolean editing(MinecraftClient mc) {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof HudEditScreen;
    }

    public static void panel(DrawContext c, Mod m, int x, int y, List<String> ls) {
        if (ls == null || ls.isEmpty()) { m.w = 0; m.h = 0; return; }
        int w = 0;
        for (String s : ls) w = Math.max(w, Gfx.w(s));
        w += 12;
        int h = ls.size() * 10 + 4;
        m.w = w; m.h = h;
        Gfx.rrect(c, x, y, w, h, 3, bg());
        c.fill(x, y + 2, x + 1, y + h - 2, Config.accent());
        for (int i = 0; i < ls.size(); i++) Gfx.shadow(c, ls.get(i), x + 6, y + 3 + i * 10, 0xFFFFFFFF);
    }

    static int px(Mod m, int sw) {
        int x = m.x < 0 ? (sw - m.w) / 2 : m.x;
        return Math.max(0, Math.min(x, Math.max(0, sw - m.w)));
    }

    static int py(Mod m, int sh) { return Math.max(0, Math.min(m.y, Math.max(0, sh - m.h))); }

    static void drag(MinecraftClient mc, int sw, int sh) {
        if (!editing(mc)) { dragging = null; wasDown = false; return; }
        long h = mc.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(h, 0) == GLFW.GLFW_PRESS;
        int mx = (int) (mc.mouse.getX() * sw / mc.getWindow().getWidth());
        int my = (int) (mc.mouse.getY() * sh / mc.getWindow().getHeight());
        if (down && !wasDown) {
            for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
                Mod m = Modules.ALL.get(i);
                if (!m.hud || m.full || !m.enabled || m.w <= 0) continue;
                int x = px(m, sw), y = py(m, sh);
                if (mx >= x && mx < x + m.w && my >= y && my < y + m.h) { dragging = m; offX = mx - x; offY = my - y; break; }
            }
        }
        if (down && dragging != null) {
            dragging.x = Math.max(0, Math.min(mx - offX, Math.max(0, sw - dragging.w)));
            dragging.y = Math.max(0, Math.min(my - offY, Math.max(0, sh - dragging.h)));
        }
        if (!down) dragging = null;
        wasDown = down;
    }

    public static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        poll(mc);
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.hudHidden) {
            int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
            drag(mc, sw, sh);
            boolean edit = editing(mc);
            for (Mod m : Modules.ALL) {
                if (!m.hud || !m.enabled) continue;
                if (m.full) { m.paint(c, 0, 0); continue; }
                int x = px(m, sw), y = py(m, sh);
                m.paint(c, x, y);
                if (edit && m.w > 0) Gfx.outline(c, x - 1, y - 1, m.w + 2, m.h + 2, dragging == m ? Config.accent() : 0x88FFFFFF);
            }
        }
        Notify.render(c);
    }
}
