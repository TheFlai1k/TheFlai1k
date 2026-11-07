package com.theflai1k;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Tick {
    private Tick() {}

    static boolean dead;
    static Set<String> prevOnline = null;
    static int onlineTimer;

    public static void run(MinecraftClient mc) {
        while (TheFlai1k.menuKey.wasPressed()) {
            if (mc.currentScreen == null && mc.world != null) mc.setScreen(new MenuScreen());
        }
        if (mc.player == null || mc.world == null) { prevOnline = null; return; }
        keys(mc);
        Tools.afkTick(mc);
        for (Mod m : Modules.ALL) if (m.enabled && m.onTick != null) m.onTick.accept(m, mc);
        // death point
        if (mc.player.getHealth() <= 0f) {
            if (!dead) {
                dead = true;
                Modules.deathText = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ() + " (" + Tools.dim() + ")";
                Notify.push("You died", Modules.deathText, 0xFFFF6B6B);
            }
        } else dead = false;
        // friends coming online / going offline
        if (++onlineTimer >= 20 && !Config.friends.isEmpty()) {
            onlineTimer = 0;
            Set<String> now = new HashSet<>();
            Set<String> all = Tools.online();
            for (String f : Config.friends) if (all.contains(f.toLowerCase(Locale.ROOT))) now.add(f.toLowerCase(Locale.ROOT));
            if (prevOnline != null) {
                for (String n : now) if (!prevOnline.contains(n)) Notify.push("Friend online", n, 0xFF6EE7A0);
                for (String n : prevOnline) if (!now.contains(n)) Notify.push("Friend left", n, 0xFF8A93A3);
            }
            prevOnline = now;
        }
    }

    static void keys(MinecraftClient mc) {
        if (mc.currentScreen != null) {
            for (Mod m : Modules.ALL) { if (m.hold) m.active = false; m.wasDown = true; }
            return;
        }
        long h = mc.getWindow().getHandle();
        for (Mod m : Modules.ALL) {
            if (m.key < 0) continue;
            boolean d = InputUtil.isKeyPressed(h, m.key);
            if (m.hold) m.active = m.enabled && d;
            else if (d && !m.wasDown) m.press(mc);
            m.wasDown = d;
        }
    }
}
