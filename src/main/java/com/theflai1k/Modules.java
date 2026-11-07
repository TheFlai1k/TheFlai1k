package com.theflai1k;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.theflai1k.Mod.Cat.*;

public final class Modules {
    private Modules() {}

    public static final List<Mod> ALL = new ArrayList<>();
    public static Mod STREAMER, NOTIF, WPSHARE, COORDSHARE, ZOOM;
    public static String deathText;
    public static double[] gps;

    static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
    static final String[] ARM_N = {"Helmet", "Chestplate", "Leggings", "Boots"};
    static final EquipmentSlot[] ARM = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    static Mod add(Mod m) { ALL.add(m); return m; }

    static int ping() {
        var h = mc().getNetworkHandler();
        if (h == null || mc().player == null) return 0;
        var e = h.getPlayerListEntry(mc().player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    static int pct(ItemStack s) {
        int max = s.getMaxDamage();
        return max <= 0 ? 100 : (max - s.getDamage()) * 100 / max;
    }

    static int pctCol(int p) { return p > 60 ? 0xFF6EE7A0 : p > 30 ? 0xFFFFD166 : 0xFFFF6B6B; }

    public static boolean isFriend(String n) {
        for (String f : Config.friends) if (f.equalsIgnoreCase(n)) return true;
        return false;
    }

    public static void disconnect(MinecraftClient mc) {
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().getConnection().disconnect(Text.literal("TheFlai1k: disconnected"));
    }

    public static void restoreFov() {
        if (ZOOM != null && ZOOM.b1) { mc().options.getFov().setValue(ZOOM.i1); ZOOM.b1 = false; }
    }

    static void key(DrawContext c, int x, int y, int w, int h, String t, boolean on) {
        Gfx.rrect(c, x, y, w, h, 3, on ? Gfx.a(Config.accent(), 0.85f) : Hud.bg());
        if (h >= 10 && !t.isEmpty()) Gfx.text(c, t, x + (w - Gfx.w(t)) / 2, y + (h - 8) / 2, on ? 0xFF0C0E12 : 0xFFFFFFFF);
    }

    static void held(Mod m, DrawContext c, int x, int y, ItemStack st) {
        String n = st.isEmpty() ? "Empty" : st.getName().getString() + (st.getCount() > 1 ? " x" + st.getCount() : "");
        if (!st.isEmpty() && st.isDamageable()) n += " " + pct(st) + "%";
        int w = Gfx.w(n) + 30;
        m.w = w; m.h = 20;
        Gfx.rrect(c, x, y, w, 20, 3, Hud.bg());
        if (!st.isEmpty()) c.drawItem(st, x + 2, y + 2);
        Gfx.shadow(c, n, x + 24, y + 6, st.isEmpty() ? 0xFF8A93A3 : 0xFFFFFFFF);
    }

    static String dist(double d) { return d >= 1000 ? String.format(Locale.ROOT, "%.1fkm", d / 1000) : (int) d + "m"; }

    public static void init() {
        // ======================= HUD =======================
        add(new Mod("FPS Display", "Current frames per second", HUD).hud(6, 6).on()
                .lines(m -> List.of("FPS: " + mc().getCurrentFps())));
        add(new Mod("Ping Display", "Latency to the server", HUD).hud(6, 24).on()
                .lines(m -> List.of("Ping: " + ping() + " ms")));
        add(new Mod("CPS Display", "Clicks per second (left | right)", HUD).hud(6, 42).on()
                .lines(m -> List.of("CPS: " + Hud.cps(0) + " | " + Hud.cps(1))));
        add(new Mod("Coordinates", "Your XYZ and facing direction", HUD).hud(6, 60).on()
                .with(new Setting("Show direction", true))
                .lines(m -> {
                    var p = mc().player;
                    List<String> l = new ArrayList<>();
                    l.add("XYZ: " + p.getBlockX() + " / " + p.getBlockY() + " / " + p.getBlockZ());
                    if (m.s("Show direction").on()) l.add("Facing: " + p.getHorizontalFacing().asString());
                    return l;
                }));
        add(new Mod("Server Info", "Nickname, server IP and time", HUD).hud(6, 94)
                .lines(m -> {
                    boolean st = STREAMER != null && STREAMER.enabled;
                    var si = mc().getCurrentServerEntry();
                    return List.of("Player: " + (st ? "hidden" : mc().getSession().getUsername()),
                            "Server: " + (si == null ? "Singleplayer" : (st ? "hidden" : si.address)),
                            "Time: " + LocalTime.now().format(HMS));
                }));
        add(new Mod("Clock", "Real time", HUD).hud(6, 130)
                .lines(m -> List.of("Time: " + LocalTime.now().format(HMS))));
        add(new Mod("Day Counter", "Minecraft day number", HUD).hud(6, 148)
                .lines(m -> List.of("Day: " + (mc().world.getTimeOfDay() / 24000L + 1))));
        add(new Mod("Angle Display", "Yaw and pitch", HUD).hud(6, 166)
                .lines(m -> List.of(String.format(Locale.ROOT, "Yaw %.1f  Pitch %.1f", MathHelper.wrapDegrees(mc().player.getYaw()), mc().player.getPitch()))));
        add(new Mod("Entity Count", "Loaded entities around you", HUD).hud(6, 184)
                .lines(m -> { int n = 0; for (Entity e : mc().world.getEntities()) n++; return List.of("Entities: " + n); }));
        add(new Mod("Saturation Display", "Food level and saturation", HUD).hud(6, 202)
                .lines(m -> { var hm = mc().player.getHungerManager(); return List.of(String.format(Locale.ROOT, "Food %d  Sat %.1f", hm.getFoodLevel(), hm.getSaturationLevel())); }));
        add(new Mod("RAM Usage", "Memory used by the game", HUD).hud(6, 220)
                .lines(m -> { Runtime r = Runtime.getRuntime(); return List.of("RAM: " + ((r.totalMemory() - r.freeMemory()) >> 20) + " / " + (r.maxMemory() >> 20) + " MB"); }));
        add(new Mod("Keystrokes", "WASD, mouse buttons and jump", HUD).hud(6, 240)
                .paint((m, c, x, y) -> {
                    var o = mc().options;
                    long h = mc().getWindow().getHandle();
                    m.w = 58; m.h = 62;
                    key(c, x + 20, y, 18, 18, "W", o.forwardKey.isPressed());
                    key(c, x, y + 20, 18, 18, "A", o.leftKey.isPressed());
                    key(c, x + 20, y + 20, 18, 18, "S", o.backKey.isPressed());
                    key(c, x + 40, y + 20, 18, 18, "D", o.rightKey.isPressed());
                    key(c, x, y + 40, 28, 12, "LMB", GLFW.glfwGetMouseButton(h, 0) == GLFW.GLFW_PRESS);
                    key(c, x + 30, y + 40, 28, 12, "RMB", GLFW.glfwGetMouseButton(h, 1) == GLFW.GLFW_PRESS);
                    key(c, x, y + 54, 58, 8, "", o.jumpKey.isPressed());
                }));
        add(new Mod("Armor HUD", "Armor pieces with durability", HUD).hud(6, 80).on()
                .with(new Setting("Show %", true))
                .paint((m, c, x, y) -> {
                    var p = mc().player;
                    m.w = 62; m.h = 18 * 4;
                    for (int i = 0; i < 4; i++) {
                        ItemStack st = p.getEquippedStack(ARM[i]);
                        int ry = y + i * 18;
                        Gfx.rrect(c, x, ry, 62, 17, 3, Hud.bg());
                        if (st.isEmpty()) continue;
                        c.drawItem(st, x + 1, ry);
                        if (m.s("Show %").on() && st.isDamageable()) {
                            int pc = pct(st);
                            Gfx.shadow(c, pc + "%", x + 20, ry + 5, pctCol(pc));
                        }
                    }
                }));
        add(new Mod("Effect HUD", "Active potion effects", HUD).hud(80, 6)
                .lines(m -> {
                    List<String> l = new ArrayList<>();
                    for (StatusEffectInstance e : mc().player.getStatusEffects()) {
                        if (e.getEffectType() == StatusEffects.NIGHT_VISION && ALL.stream().anyMatch(x -> x.name.equals("Fullbright") && x.enabled)) continue;
                        int s = e.getDuration() / 20;
                        String t = e.isInfinite() ? "inf" : (s / 60) + ":" + String.format("%02d", s % 60);
                        l.add(e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1) + "  " + t);
                    }
                    return l;
                }));
        add(new Mod("Hand Item", "Item in your main hand", HUD).hud(6, 262)
                .paint((m, c, x, y) -> held(m, c, x, y, mc().player.getMainHandStack())));
        add(new Mod("Offhand Item", "Item in your off hand", HUD).hud(6, 286)
                .paint((m, c, x, y) -> held(m, c, x, y, mc().player.getOffHandStack())));
        add(new Mod("Coordinate Bar", "Compass bar with directions", HUD).center(4)
                .paint((m, c, x, y) -> {
                    m.w = 180; m.h = 22;
                    Gfx.rrect(c, x, y, 180, 22, 4, Hud.bg());
                    float heading = MathHelper.wrapDegrees(mc().player.getYaw() + 180f);
                    if (heading < 0) heading += 360f;
                    String[] N = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
                    for (int d = 0; d < 360; d += 15) {
                        float rel = MathHelper.wrapDegrees(d - heading);
                        if (Math.abs(rel) > 84) continue;
                        int px = x + 90 + Math.round(rel);
                        if (d % 45 == 0) {
                            String s = N[d / 45];
                            Gfx.shadow(c, s, px - Gfx.w(s) / 2, y + 3, d % 90 == 0 ? Config.accent() : 0xFFFFFFFF);
                        } else c.fill(px, y + 6, px + 1, y + 10, 0x88FFFFFF);
                        c.fill(px, y + 13, px + 1, y + 16, 0x66FFFFFF);
                    }
                    c.fill(x + 89, y + 17, x + 92, y + 20, Config.accent());
                }));
        add(new Mod("GPS", "Arrow and distance to a target (.gps x y z)", HUD).hud(6, 310)
                .lines(m -> {
                    if (gps == null) return List.of();
                    var p = mc().player;
                    double dx = gps[0] + 0.5 - p.getX(), dz = gps[2] + 0.5 - p.getZ();
                    double d = Math.sqrt(dx * dx + dz * dz);
                    float heading = MathHelper.wrapDegrees(p.getYaw() + 180f);
                    float target = (float) Math.toDegrees(Math.atan2(dx, -dz));
                    float delta = MathHelper.wrapDegrees(target - heading);
                    String arrow = Math.abs(delta) < 22 ? "^" : Math.abs(delta) > 135 ? "v" : delta > 0 ? (delta > 67 ? ">>" : ">") : (delta < -67 ? "<<" : "<");
                    return List.of("GPS " + arrow + "  " + dist(d), (int) gps[0] + " " + (int) gps[1] + " " + (int) gps[2]);
                }));
        add(new Mod("Death Info", "Where you died last", HUD).hud(6, 340)
                .lines(m -> deathText == null ? List.of() : List.of("Death: " + deathText)));
        add(new Mod("Online Friends HUD", "Friends currently online", HUD).hud(120, 6)
                .lines(m -> {
                    List<String> l = new ArrayList<>();
                    Set<String> on = Tools.online();
                    for (String f : Config.friends) if (on.contains(f.toLowerCase(Locale.ROOT))) l.add("\u00a7a+ \u00a7f" + f);
                    if (l.isEmpty()) l.add("Friends: none online");
                    else l.add(0, "Friends online");
                    return l;
                }));
        add(new Mod("Waypoints HUD", "Nearest waypoints and distance", HUD).hud(120, 60)
                .lines(m -> {
                    List<Config.Waypoint> ws = new ArrayList<>();
                    String d = Tools.dim();
                    for (Config.Waypoint w : Config.waypoints) if (w.dim.equals(d)) ws.add(w);
                    ws.sort(Comparator.comparingDouble(Tools::dist));
                    List<String> l = new ArrayList<>();
                    for (int i = 0; i < Math.min(4, ws.size()); i++) l.add(ws.get(i).name + "  " + dist(Tools.dist(ws.get(i))));
                    return l;
                }));
        add(new Mod("AFK Timer", "Time since you last moved", HUD).hud(120, 120)
                .with(new Setting("Show after (s)", 5, 0, 120, 1))
                .lines(m -> {
                    long s = (System.currentTimeMillis() - Tools.lastActive) / 1000;
                    if (s < m.s("Show after (s)").i()) return List.of("AFK: active");
                    return List.of("AFK: " + (s / 60) + ":" + String.format("%02d", s % 60));
                }));
        add(new Mod("Spotify", "Now playing from the Spotify desktop app (Windows)", HUD).hud(120, 140)
                .act("Play / Pause", mc -> Tools.media(179))
                .act("Next track", mc -> Tools.media(176))
                .act("Previous track", mc -> Tools.media(177))
                .tick((m, mc) -> Tools.spotifyTick())
                .lines(m -> {
                    String t = Tools.spotifyTrack;
                    if (t.isEmpty()) return List.of("Spotify: paused / not running");
                    return List.of("Now playing", Gfx.fit(t, 200));
                }));
        add(new Mod("Minimap", "Top-down map with waypoints and friends", HUD).hud(9999, 4)
                .with(new Setting("Radius", 24, 12, 48, 4))
                .with(new Setting("Cell size", 2, 1, 3, 1))
                .with(new Setting("Show waypoints", true))
                .paint((m, c, x, y) -> {
                    int r = m.s("Radius").i(), cell = m.s("Cell size").i(), n = r * 2, size = n * cell;
                    m.w = size + 4; m.h = size + 4;
                    int[] map = Tools.minimap(r);
                    Gfx.rrect(c, x, y, size + 4, size + 4, 4, Hud.bg());
                    for (int j = 0; j < n; j++)
                        for (int i = 0; i < n; i++) {
                            int col = map[j * n + i];
                            if (col == 0) continue;
                            c.fill(x + 2 + i * cell, y + 2 + j * cell, x + 2 + (i + 1) * cell, y + 2 + (j + 1) * cell, col);
                        }
                    var p = mc().player;
                    int px = p.getBlockX(), pz = p.getBlockZ();
                    if (m.s("Show waypoints").on()) {
                        String d = Tools.dim();
                        for (Config.Waypoint w : Config.waypoints) {
                            if (!w.dim.equals(d)) continue;
                            int dx = w.x - px, dz = w.z - pz;
                            if (Math.abs(dx) >= r || Math.abs(dz) >= r) continue;
                            int sx = x + 2 + (dx + r) * cell, sy = y + 2 + (dz + r) * cell;
                            c.fill(sx - 1, sy - 1, sx + 3, sy + 3, 0xFFFFD166);
                        }
                    }
                    for (PlayerEntity o : mc().world.getPlayers()) {
                        if (o == p) continue;
                        int dx = o.getBlockX() - px, dz = o.getBlockZ() - pz;
                        if (Math.abs(dx) >= r || Math.abs(dz) >= r) continue;
                        int sx = x + 2 + (dx + r) * cell, sy = y + 2 + (dz + r) * cell;
                        c.fill(sx - 1, sy - 1, sx + 2, sy + 2, isFriend(o.getName().getString()) ? 0xFF6EE7A0 : 0xFFFF6B6B);
                    }
                    int cx = x + 2 + r * cell, cy = y + 2 + r * cell;
                    double yaw = Math.toRadians(p.getYaw());
                    for (int k = 2; k <= 6; k++) {
                        int fx = cx + (int) Math.round(-Math.sin(yaw) * k), fy = cy + (int) Math.round(Math.cos(yaw) * k);
                        c.fill(fx, fy, fx + 1, fy + 1, 0xCCFFFFFF);
                    }
                    c.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
                    Gfx.shadow(c, "N", x + size / 2, y + 3, Config.accent());
                }));

        // ======================= COMBAT =======================
        add(new Mod("Hitboxes", "Show entity hitboxes", COMBAT)
                .tick((m, mc) -> mc.getEntityRenderDispatcher().setRenderHitboxes(true))
                .disable(m -> mc().getEntityRenderDispatcher().setRenderHitboxes(false)));
        add(new Mod("Auto Leave", "Disconnect when health is low", COMBAT)
                .with(new Setting("Health (hearts)", 3, 1, 10, 1))
                .tick((m, mc) -> {
                    float hp = mc.player.getHealth();
                    if (hp > 0 && hp <= m.s("Health (hearts)").i() * 2) {
                        m.setEnabled(false);
                        disconnect(mc);
                    }
                }));
        add(new Mod("Quick Disconnect", "Bind a key to leave the server instantly", COMBAT).on()
                .keyAct((m, mc) -> disconnect(mc)));
        add(new Mod("Player Info", "Nearby players with their health", COMBAT).hud(260, 6)
                .with(new Setting("Range", 48, 8, 128, 4))
                .lines(m -> {
                    var me = mc().player;
                    double rng = m.s("Range").value;
                    List<PlayerEntity> ps = new ArrayList<>();
                    for (PlayerEntity p : mc().world.getPlayers()) if (p != me && me.distanceTo(p) <= rng) ps.add(p);
                    ps.sort(Comparator.comparingDouble(p -> me.distanceTo(p)));
                    List<String> out = new ArrayList<>();
                    for (int i = 0; i < Math.min(6, ps.size()); i++) {
                        PlayerEntity p = ps.get(i);
                        String n = p.getName().getString();
                        float hp = p.getHealth();
                        String col = hp >= 14 ? "\u00a7a" : hp >= 7 ? "\u00a7e" : "\u00a7c";
                        out.add((isFriend(n) ? "\u00a7b" : "\u00a7f") + n + " " + col + Math.round(hp) + " HP");
                    }
                    return out;
                }));
        add(new Mod("Low Durability Alert", "Warns when armor or your tool is about to break", COMBAT).on()
                .with(new Setting("Threshold %", 15, 5, 50, 1))
                .tick((m, mc) -> {
                    long now = System.currentTimeMillis();
                    if (now - m.t1 < 30000) return;
                    int thr = m.s("Threshold %").i();
                    var p = mc.player;
                    for (int i = 0; i < 4; i++) {
                        ItemStack s = p.getEquippedStack(ARM[i]);
                        if (s.isDamageable() && pct(s) <= thr) { Notify.push("Low durability", ARM_N[i] + " " + pct(s) + "%", 0xFFFF6B6B); m.t1 = now; return; }
                    }
                    ItemStack h = p.getMainHandStack();
                    if (h.isDamageable() && pct(h) <= thr) { Notify.push("Low durability", h.getName().getString() + " " + pct(h) + "%", 0xFFFFD166); m.t1 = now; }
                })
                .full()
                .paint((m, c, x, y) -> {
                    var p = mc().player;
                    int thr = m.s("Threshold %").i();
                    List<String> low = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        ItemStack s = p.getEquippedStack(ARM[i]);
                        if (s.isDamageable() && pct(s) <= thr) low.add(ARM_N[i] + " " + pct(s) + "%");
                    }
                    if (low.isEmpty() || (System.currentTimeMillis() / 400) % 2 == 1) return;
                    String t = "LOW DURABILITY: " + String.join(", ", low);
                    int sw = c.getScaledWindowWidth(), w = Gfx.w(t) + 12;
                    Gfx.rrect(c, (sw - w) / 2, 34, w, 16, 4, 0xCC3A0F12);
                    Gfx.shadow(c, t, (sw - w) / 2 + 6, 38, 0xFFFF6B6B);
                }));
        add(new Mod("Low Health Alert", "Configurable low health warning", COMBAT).on()
                .with(new Setting("Health (hearts)", 4, 1, 10, 1))
                .tick((m, mc) -> {
                    float hp = mc.player.getHealth();
                    long now = System.currentTimeMillis();
                    if (hp > 0 && hp <= m.s("Health (hearts)").i() * 2 && now - m.t1 > 10000) {
                        Notify.push("Low health", Math.round(hp) + " HP left!", 0xFFFF6B6B);
                        m.t1 = now;
                    }
                })
                .full()
                .paint((m, c, x, y) -> {
                    float hp = mc().player.getHealth();
                    if (hp <= 0 || hp > m.s("Health (hearts)").i() * 2 || (System.currentTimeMillis() / 350) % 2 == 1) return;
                    String t = "LOW HEALTH  " + Math.round(hp) + " HP";
                    int sw = c.getScaledWindowWidth(), w = Gfx.w(t) + 12;
                    Gfx.rrect(c, (sw - w) / 2, 56, w, 16, 4, 0xCC3A0F12);
                    Gfx.shadow(c, t, (sw - w) / 2 + 6, 60, 0xFFFF6B6B);
                }));
        add(new Mod("Low HP Vignette", "Red screen edges when you have under 4 hearts", COMBAT)
                .with(new Setting("Health (hearts)", 4, 1, 10, 1))
                .full()
                .paint((m, c, x, y) -> {
                    float hp = mc().player.getHealth();
                    float thr = m.s("Health (hearts)").i() * 2;
                    if (hp <= 0 || hp > thr) return;
                    float k = 1f - hp / thr;
                    int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
                    int step = Math.max(2, sw / 80);
                    for (int i = 0; i < 16; i++) {
                        float f = (1f - i / 16f);
                        int col = (((int) (170 * k * f * f)) << 24) | 0xC01010;
                        int t = i * step;
                        c.fill(t, t, sw - t, t + step, col);
                        c.fill(t, sh - t - step, sw - t, sh - t, col);
                        c.fill(t, t + step, t + step, sh - t - step, col);
                        c.fill(sw - t - step, t + step, sw - t, sh - t - step, col);
                    }
                }));

        // ======================= MOVEMENT =======================
        add(new Mod("Toggle Sprint", "Sprint automatically while moving forward", MOVEMENT)
                .tick((m, mc) -> {
                    var p = mc.player;
                    if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision && p.getHungerManager().getFoodLevel() > 6) p.setSprinting(true);
                }));
        add(new Mod("Toggle Sneak", "Press once to sneak, press again to stop", MOVEMENT)
                .tick((m, mc) -> {
                    long h = mc.getWindow().getHandle();
                    int code = KeyBindingHelper.getBoundKeyOf(mc.options.sneakKey).getCode();
                    boolean d = mc.currentScreen == null && InputUtil.isKeyPressed(h, code);
                    if (d && !m.b2) m.b1 = !m.b1;
                    m.b2 = d;
                    mc.options.sneakKey.setPressed(m.b1 || d);
                })
                .disable(m -> { m.b1 = false; mc().options.sneakKey.setPressed(false); }));
        add(new Mod("Safe Walk", "Stops you at the edge of blocks", MOVEMENT)
                .tick((m, mc) -> {
                    var p = mc.player;
                    if (!p.isOnGround() || p.isSneaking() || p.getAbilities().flying) return;
                    Vec3d v = p.getVelocity();
                    if (Math.abs(v.x) + Math.abs(v.z) < 0.01) return;
                    BlockPos bp = BlockPos.ofFloored(p.getX() + v.x * 3, p.getY() - 0.5, p.getZ() + v.z * 3);
                    if (mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) p.setVelocity(0, v.y, 0);
                }));
        add(new Mod("Flight Speed", "Creative flight speed", MOVEMENT)
                .with(new Setting("Speed", 0.1, 0.05, 0.5, 0.01))
                .tick((m, mc) -> { var ab = mc.player.getAbilities(); if (ab.flying) ab.setFlySpeed(m.s("Speed").f()); })
                .disable(m -> { if (mc().player != null) mc().player.getAbilities().setFlySpeed(0.05f); }));

        // ======================= VISUAL =======================
        add(new Mod("Fullbright", "Everything is bright (no darkness)", VISUAL).pin()
                .tick((m, mc) -> {
                    var e = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                    if (e == null || e.getDuration() < 600)
                        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 2000, 0, false, false, false));
                })
                .disable(m -> { if (mc().player != null) mc().player.removeStatusEffect(StatusEffects.NIGHT_VISION); }));
        ZOOM = add(new Mod("Zoom", "Hold the key to zoom in (Optifine style)", VISUAL).on().pin().holdKey(GLFW.GLFW_KEY_C)
                .with(new Setting("Zoom FOV", 30, 30, 70, 1))
                .tick((m, mc) -> {
                    var o = mc.options.getFov();
                    int cur = o.getValue(), tgt;
                    if (m.active) { if (!m.b1) { m.b1 = true; m.i1 = cur; } tgt = m.s("Zoom FOV").i(); }
                    else if (m.b1) tgt = m.i1;
                    else return;
                    int next = cur + Math.round((tgt - cur) * 0.5f);
                    if (next == cur && cur != tgt) next += tgt > cur ? 1 : -1;
                    if (next != cur) o.setValue(next);
                    if (!m.active && next == tgt) m.b1 = false;
                })
                .disable(m -> restoreFov()));
        add(new Mod("Clouds Off", "Hides clouds", VISUAL)
                .tick((m, mc) -> { var o = mc.options.getCloudRenderMode(); if (o.getValue() != CloudRenderMode.OFF) o.setValue(CloudRenderMode.OFF); })
                .disable(m -> mc().options.getCloudRenderMode().setValue(CloudRenderMode.FANCY)));
        add(new Mod("Shadows Off", "Disables entity shadows (more FPS)", VISUAL)
                .tick((m, mc) -> { var o = mc.options.getEntityShadows(); if (o.getValue()) o.setValue(false); })
                .disable(m -> mc().options.getEntityShadows().setValue(true)));
        add(new Mod("Minimal Particles", "Reduces particles (more FPS)", VISUAL)
                .tick((m, mc) -> { var o = mc.options.getParticles(); if (o.getValue() != ParticlesMode.MINIMAL) o.setValue(ParticlesMode.MINIMAL); })
                .disable(m -> mc().options.getParticles().setValue(ParticlesMode.ALL)));
        add(new Mod("Aura Cosmetics", "Decorative particle aura around you (only you see it)", VISUAL)
                .with(new Setting("Style (0-4)", 0, 0, 4, 1))
                .with(new Setting("Radius", 0.8, 0.4, 2.0, 0.1))
                .tick((m, mc) -> Tools.aura(mc, m)));

        // ======================= WORLD =======================
        add(new Mod("No Weather", "Removes rain and thunder visuals", WORLD)
                .tick((m, mc) -> { mc.world.setRainGradient(0f); mc.world.setThunderGradient(0f); }));
        add(new Mod("Render Distance", "Set render distance quickly", WORLD)
                .with(new Setting("Chunks", 12, 2, 32, 1))
                .tick((m, mc) -> { var o = mc.options.getViewDistance(); int v = m.s("Chunks").i(); if (o.getValue() != v) o.setValue(v); }));

        // ======================= UTILITY =======================
        STREAMER = add(new Mod("Streamer Mode", "Hides nickname and server IP on your HUD", UTILITY).pin());
        NOTIF = add(new Mod("Custom Notifications", "Pop-up notifications of the client", UTILITY).on().pin()
                .with(new Setting("Notification time (s)", 4, 1, 10, 1)));
        COORDSHARE = add(new Mod("Coordinate Sharing", "Key sends your coords to chat; incoming coords are detected", UTILITY).on()
                .with(new Setting("Send to chat", true))
                .keyAct((m, mc) -> { if (m.s("Send to chat").on()) Tools.sendCoords(); else Tools.copyCoords(); }));
        WPSHARE = add(new Mod("Waypoint Sharing", "Key shares your nearest waypoint in chat", UTILITY).on()
                .with(new Setting("Auto accept", false))
                .keyAct((m, mc) -> {
                    Config.Waypoint best = null; double bd = 1e18;
                    for (Config.Waypoint w : Config.waypoints) { double d = Tools.dist(w); if (d < bd) { bd = d; best = w; } }
                    if (best == null) Notify.push("Waypoint sharing", "No waypoints yet", 0xFFFF6B6B); else Tools.shareWp(best);
                }));
        add(new Mod("Screenshot Sharing", "Key copies the latest screenshot path (and opens it)", UTILITY).on()
                .with(new Setting("Open file", true))
                .keyAct((m, mc) -> Tools.shareLatest(mc, m.s("Open file").on())));
        add(new Mod("Radial Menu", "Circular quick menu of pinned modules", UTILITY).on().defKey(GLFW.GLFW_KEY_V)
                .keyAct((m, mc) -> mc.setScreen(new RadialScreen(m.key))));
        add(new Mod("Inventory Layout Presets", "Save and restore your hotbar layout", UTILITY)
                .act("Save layout 1", mc -> Tools.saveLayout("1")).act("Apply layout 1", mc -> Tools.applyLayout("1"))
                .act("Save layout 2", mc -> Tools.saveLayout("2")).act("Apply layout 2", mc -> Tools.applyLayout("2"))
                .act("Save layout 3", mc -> Tools.saveLayout("3")).act("Apply layout 3", mc -> Tools.applyLayout("3")));
        add(new Mod("Friend List", "Your own friend list", UTILITY).opens(3, 0));
        add(new Mod("Waypoints Manager", "Create, delete and sort points of interest", UTILITY).opens(3, 1));
        add(new Mod("Block Search", "Find blocks by name", UTILITY).opens(1, 0));
        add(new Mod("Block Preview", "View a block in a separate window", UTILITY).opens(1, 0));
        add(new Mod("Build Palette", "Save block palettes for building", UTILITY).opens(1, 1));
        add(new Mod("Color Picker", "Pick colors and find matching blocks", UTILITY).opens(1, 2));
        add(new Mod("Screenshot Gallery", "Built-in screenshot gallery", UTILITY).opens(2, 0));
        add(new Mod("Screenshot Editor", "Crop and adjust screenshots", UTILITY).opens(2, 0));
        add(new Mod("Screenshot Blueprint", "Turn a screenshot into a blueprint", UTILITY).opens(2, 0));
        add(new Mod("In-game Notes", "Notepad inside the game", UTILITY).opens(4, 0));
        add(new Mod("Keybind Conflict Detector", "Find clashing hotkeys", UTILITY).opens(5, 0));
    }
}
