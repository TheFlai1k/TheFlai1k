package com.theflai1k;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Gfx {
    private Gfx() {}

    public static int a(int col, float f) {
        int al = Math.max(0, Math.min(255, (int) ((col >>> 24) * f)));
        return (al << 24) | (col & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = (a >>> s) & 255, cb = (b >>> s) & 255;
            r |= (((int) (ca + (cb - ca) * t)) & 255) << s;
        }
        return r;
    }

    public static int w(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }
    public static String fit(String s, int w) { return MinecraftClient.getInstance().textRenderer.trimToWidth(s, Math.max(0, w)); }
    public static String fitEnd(String s, int w) { return MinecraftClient.getInstance().textRenderer.trimToWidth(s, Math.max(0, w), true); }

    public static void text(DrawContext c, String s, int x, int y, int col) {
        if ((col >>> 24) < 8) return;
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, col, false);
    }

    public static void shadow(DrawContext c, String s, int x, int y, int col) {
        if ((col >>> 24) < 8) return;
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, col, true);
    }

    public static void bold(DrawContext c, String s, int x, int y, int col) {
        if ((col >>> 24) < 8) return;
        c.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(s).formatted(Formatting.BOLD), x, y, col, false);
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if ((col >>> 24) == 0 || w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double d = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - d * d));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, col);
        }
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col);
        c.fill(x + w - 1, y, x + w, y + h, col);
    }

    public static void toggle(DrawContext c, int x, int y, float anim, float fade) {
        rrect(c, x, y, 22, 11, 5, a(mix(0xFF2E333B, Config.accent(), anim), fade));
        int kx = x + 1 + (int) (11 * anim);
        rrect(c, kx, y + 1, 9, 9, 4, a(mix(0xFFB8BEC9, 0xFF0D0F13, anim), fade));
    }

    public static String keyName(int code) {
        try { return InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString(); }
        catch (Exception e) { return "?"; }
    }
}
