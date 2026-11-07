package com.theflai1k;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Tools {
    private Tools() {}

    static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static String dim() {
        MinecraftClient mc = mc();
        return mc.world == null ? "overworld" : mc.world.getRegistryKey().getValue().getPath();
    }

    public static void say(String s) {
        MinecraftClient mc = mc();
        if (mc.player != null) mc.player.sendMessage(Text.literal("\u00a77[\u00a7bTheFlai1k\u00a77] \u00a7f" + s), false);
    }

    public static void clip(String s) { mc().keyboard.setClipboard(s); }

    public static void shutdown() { Modules.restoreFov(); }

    // ================= BLOCKS =================
    public static final class BlockInfo {
        public final Block b; public final String name, id; public final ItemStack st; public final int color;
        BlockInfo(Block b, String name, String id, ItemStack st, int color) { this.b = b; this.name = name; this.id = id; this.st = st; this.color = color; }
    }

    private static List<BlockInfo> blocks;

    public static List<BlockInfo> blocks() {
        if (blocks == null) {
            blocks = new ArrayList<>();
            for (Block b : Registries.BLOCK) {
                Item it = b.asItem();
                if (it == Items.AIR) continue;
                String id = Registries.BLOCK.getId(b).getPath();
                int col = 0;
                try { col = b.getDefaultMapColor().color; } catch (Exception ignored) {}
                blocks.add(new BlockInfo(b, b.getName().getString(), id, new ItemStack(it), col));
            }
            blocks.sort(Comparator.comparing((BlockInfo x) -> x.name));
        }
        return blocks;
    }

    public static BlockInfo findBlock(String id) {
        for (BlockInfo b : blocks()) if (b.id.equals(id)) return b;
        return null;
    }

    public static String cur = "Default";

    public static List<String> pal(String name) { return Config.palettes.computeIfAbsent(name, k -> new ArrayList<>()); }

    public static void addToPalette(BlockInfo b) {
        List<String> l = pal(cur);
        if (!l.contains(b.id)) l.add(b.id);
        Notify.push("Palette: " + cur, b.name + " added", 0xFF6EE7A0);
    }

    // ================= COLOR =================
    public static final Setting CR = new Setting("R", 90, 0, 255, 1), CG = new Setting("G", 140, 0, 255, 1), CB = new Setting("B", 220, 0, 255, 1);
    private static int lastRgb = -1;
    private static List<BlockInfo> near = new ArrayList<>();

    public static int rgb() { return (CR.i() << 16) | (CG.i() << 8) | CB.i(); }
    public static void setRgb(int c) { CR.value = (c >> 16) & 255; CG.value = (c >> 8) & 255; CB.value = c & 255; }
    static long colorDist(int a, int b) {
        long dr = ((a >> 16) & 255) - ((b >> 16) & 255), dg = ((a >> 8) & 255) - ((b >> 8) & 255), db = (a & 255) - (b & 255);
        return dr * dr + dg * dg + db * db;
    }

    public static List<BlockInfo> nearest(int rgb) {
        if (rgb != lastRgb) {
            lastRgb = rgb;
            List<BlockInfo> l = new ArrayList<>();
            for (BlockInfo b : blocks()) if (b.color != 0) l.add(b);
            l.sort(Comparator.comparingLong((BlockInfo b) -> colorDist(b.color, rgb)));
            near = new ArrayList<>(l.subList(0, Math.min(10, l.size())));
        }
        return near;
    }

    public static int hueShift(int rgb, float deg) {
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
        float h = hsb[0] + deg / 360f;
        h = h - (float) Math.floor(h);
        return Color.HSBtoRGB(h, hsb[1], hsb[2]) & 0xFFFFFF;
    }

    // ================= SCREENSHOTS =================
    public static final int TW = 96, TH = 54;
    public static List<File> shots = new ArrayList<>();
    public static File selFile;
    public static int[] thumb;
    public static final Setting CL = new Setting("Crop left %", 0, 0, 45, 1), CRT = new Setting("Crop right %", 0, 0, 45, 1),
            CT = new Setting("Crop top %", 0, 0, 45, 1), CB2 = new Setting("Crop bottom %", 0, 0, 45, 1),
            BR = new Setting("Brightness", 0, -60, 60, 1), GRAY = new Setting("Grayscale", false);

    public static File shotDir() { return new File(mc().runDirectory, "screenshots"); }

    public static void refreshShots() {
        File[] fs = shotDir().listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png"));
        shots = fs == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(fs));
        shots.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
    }

    public static void select(File f) {
        selFile = f; thumb = null;
        try {
            BufferedImage im = ImageIO.read(f);
            if (im == null) return;
            int w = im.getWidth(), h = im.getHeight();
            thumb = new int[TW * TH];
            for (int y = 0; y < TH; y++)
                for (int x = 0; x < TW; x++)
                    thumb[y * TW + x] = im.getRGB(Math.min(w - 1, x * w / TW + w / (2 * TW)), Math.min(h - 1, y * h / TH + h / (2 * TH))) | 0xFF000000;
        } catch (Exception e) { thumb = null; }
    }

    static String stamp() { return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); }
    static int cl(int v) { return Math.max(0, Math.min(255, v)); }

    public static String saveEdited() {
        if (selFile == null) return "Select a screenshot first";
        try {
            BufferedImage im = ImageIO.read(selFile);
            int w = im.getWidth(), h = im.getHeight();
            int x0 = w * CL.i() / 100, x1 = w - w * CRT.i() / 100, y0 = h * CT.i() / 100, y1 = h - h * CB2.i() / 100;
            if (x1 - x0 < 8 || y1 - y0 < 8) return "Crop is too small";
            BufferedImage out = new BufferedImage(x1 - x0, y1 - y0, BufferedImage.TYPE_INT_RGB);
            int br = BR.i() * 2;
            boolean gray = GRAY.on();
            for (int y = y0; y < y1; y++)
                for (int x = x0; x < x1; x++) {
                    int p = im.getRGB(x, y);
                    int r = cl(((p >> 16) & 255) + br), g = cl(((p >> 8) & 255) + br), b = cl((p & 255) + br);
                    if (gray) { int v = (r + g + b) / 3; r = g = b = v; }
                    out.setRGB(x - x0, y - y0, (r << 16) | (g << 8) | b);
                }
            File dir = new File(shotDir(), "edited");
            dir.mkdirs();
            File f = new File(dir, "edit_" + stamp() + ".png");
            ImageIO.write(out, "png", f);
            return "Saved: edited/" + f.getName();
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public static String blueprint() {
        if (selFile == null) return "Select a screenshot first";
        try {
            BufferedImage im = ImageIO.read(selFile);
            int sw = Math.min(960, im.getWidth());
            int sh = im.getHeight() * sw / im.getWidth();
            int[] gr = new int[sw * sh];
            for (int y = 0; y < sh; y++)
                for (int x = 0; x < sw; x++) {
                    int p = im.getRGB(x * im.getWidth() / sw, y * im.getHeight() / sh);
                    gr[y * sw + x] = (((p >> 16) & 255) * 3 + ((p >> 8) & 255) * 6 + (p & 255)) / 10;
                }
            BufferedImage out = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < sh; y++)
                for (int x = 0; x < sw; x++) {
                    int bgc = 0x0B2A5B;
                    if (x % 32 == 0 || y % 32 == 0) bgc = 0x14407F;
                    int e = 0;
                    if (x > 0 && y > 0 && x < sw - 1 && y < sh - 1) {
                        int gx = -gr[(y - 1) * sw + x - 1] + gr[(y - 1) * sw + x + 1] - 2 * gr[y * sw + x - 1] + 2 * gr[y * sw + x + 1] - gr[(y + 1) * sw + x - 1] + gr[(y + 1) * sw + x + 1];
                        int gy = -gr[(y - 1) * sw + x - 1] - 2 * gr[(y - 1) * sw + x] - gr[(y - 1) * sw + x + 1] + gr[(y + 1) * sw + x - 1] + 2 * gr[(y + 1) * sw + x] + gr[(y + 1) * sw + x + 1];
                        e = Math.min(255, (int) Math.sqrt(gx * gx + gy * gy));
                    }
                    int v = e > 40 ? e : 0;
                    int r = cl(((bgc >> 16) & 255) + v * 2 / 3), g = cl(((bgc >> 8) & 255) + v), b = cl((bgc & 255) + v);
                    out.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            try {
                Graphics2D g2 = out.createGraphics();
                g2.setColor(new Color(200, 230, 255));
                var p = mc().player;
                String cap = "TheFlai1k blueprint  " + (p == null ? "" : p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ() + "  " + dim()) + "  " + stamp();
                g2.drawString(cap, 10, sh - 10);
                g2.dispose();
            } catch (Throwable ignored) {}
            File dir = new File(shotDir(), "blueprints");
            dir.mkdirs();
            File f = new File(dir, "bp_" + stamp() + ".png");
            ImageIO.write(out, "png", f);
            return "Blueprint saved: blueprints/" + f.getName();
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public static void openFile(File f) { try { Util.getOperatingSystem().open(f); } catch (Throwable ignored) {} }

    public static void shareLatest(MinecraftClient mc, boolean open) {
        refreshShots();
        if (shots.isEmpty()) { Notify.push("Screenshot", "No screenshots found (press F2 first)", 0xFFFF6B6B); return; }
        File f = shots.get(0);
        clip(f.getAbsolutePath());
        Notify.push("Screenshot ready", "Path copied: " + f.getName(), 0xFF7AA2FF);
        if (open) openFile(f);
    }

    // ================= SPOTIFY (Windows) =================
    public static volatile String spotifyTrack = "";
    private static long spPoll;
    private static volatile boolean spBusy;

    public static void spotifyTick() {
        long n = System.currentTimeMillis();
        if (spBusy || n - spPoll < 2500) return;
        spPoll = n; spBusy = true;
        Thread t = new Thread(() -> {
            try { spotifyTrack = readSpotify(); } catch (Exception e) { spotifyTrack = ""; } finally { spBusy = false; }
        }, "theflai1k-spotify");
        t.setDaemon(true);
        t.start();
    }

    static String readSpotify() throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return "Windows only";
        Process p = new ProcessBuilder("tasklist", "/v", "/fi", "imagename eq Spotify.exe", "/fo", "csv", "/nh").redirectErrorStream(true).start();
        String best = "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
            String ln;
            while ((ln = r.readLine()) != null) {
                String[] parts = ln.split("\",\"");
                if (parts.length >= 9) {
                    String title = parts[parts.length - 1].replace("\"", "").trim();
                    if (title.contains(" - ") && !title.equals("N/A")) best = title;
                }
            }
        }
        return best;
    }

    public static void media(int code) {
        Thread t = new Thread(() -> {
            try {
                if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return;
                new ProcessBuilder("powershell", "-NoProfile", "-Command", "(New-Object -ComObject WScript.Shell).SendKeys([char]" + code + ")").start();
            } catch (Exception ignored) {}
        }, "theflai1k-media");
        t.setDaemon(true);
        t.start();
    }

    // ================= MINIMAP =================
    static int[] mm; static int mmR = -1; static long mmT;

    public static int[] minimap(int r) {
        long now = System.currentTimeMillis();
        if (mm != null && mmR == r && now - mmT < 500) return mm;
        mmT = now; mmR = r;
        MinecraftClient mc = mc();
        int n = r * 2;
        int[] out = new int[n * n];
        var w = mc.world; var p = mc.player;
        if (w == null || p == null) return out;
        int px = p.getBlockX(), pz = p.getBlockZ(), py = p.getBlockY();
        boolean sky = w.isSkyVisible(p.getBlockPos());
        BlockPos.Mutable mp = new BlockPos.Mutable();
        for (int dz = -r; dz < r; dz++)
            for (int dx = -r; dx < r; dx++) {
                int x = px + dx, z = pz + dz, col = 0;
                if (w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                    int top = sky ? w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1 : py + 2;
                    int steps = sky ? 3 : 20;
                    for (int y = top, s = 0; s < steps && y >= w.getBottomY(); y--, s++) {
                        mp.set(x, y, z);
                        BlockState st = w.getBlockState(mp);
                        if (st.isAir()) continue;
                        int c = st.getMapColor(w, mp).color;
                        if (c != 0) { col = 0xFF000000 | c; break; }
                    }
                }
                out[(dz + r) * n + (dx + r)] = col;
            }
        mm = out;
        return out;
    }

    // ================= WAYPOINTS / SHARING =================
    static final Pattern WP = Pattern.compile("\\[WP\\|([^|\\]]{1,24})\\|(-?\\d+)\\|(-?\\d+)\\|(-?\\d+)\\]");
    static final Pattern XYZ = Pattern.compile("XYZ:?\\s*(-?\\d+)[ ,/]+(-?\\d+)[ ,/]+(-?\\d+)");
    public static Config.Waypoint pending;
    public static int[] shared;

    public static double dist(Config.Waypoint wp) {
        var p = mc().player;
        if (p == null) return 0;
        double dx = wp.x + 0.5 - p.getX(), dz = wp.z + 0.5 - p.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static Config.Waypoint addWaypoint(String name, int x, int y, int z) {
        Config.Waypoint wp = new Config.Waypoint();
        wp.name = name; wp.x = x; wp.y = y; wp.z = z; wp.dim = dim(); wp.t = System.currentTimeMillis();
        Config.waypoints.add(wp);
        return wp;
    }

    public static void sendCoords() {
        var p = mc().player;
        if (p == null) return;
        String s = "XYZ: " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ() + " (" + dim() + ")";
        p.networkHandler.sendChatMessage(s);
    }

    public static void copyCoords() {
        var p = mc().player;
        if (p == null) return;
        clip(p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ());
        Notify.push("Coordinates", "Copied to clipboard", 0xFF7AA2FF);
    }

    public static void shareWp(Config.Waypoint wp) {
        var p = mc().player;
        if (p == null || wp == null) return;
        p.networkHandler.sendChatMessage("[WP|" + wp.name.replace('|', ' ') + "|" + wp.x + "|" + wp.y + "|" + wp.z + "]");
    }

    public static void acceptPending() {
        if (pending == null) { say("No incoming waypoint."); return; }
        Config.waypoints.add(pending);
        Notify.push("Waypoint saved", pending.name, 0xFF6EE7A0);
        pending = null;
    }

    public static void incoming(String msg) {
        try {
            MinecraftClient mc = mc();
            if (mc.player == null) return;
            String me = mc.getSession().getUsername();
            if (me != null && msg.contains(me)) return;
            Matcher m = WP.matcher(msg);
            if (m.find()) {
                Config.Waypoint wp = new Config.Waypoint();
                wp.name = m.group(1); wp.x = Integer.parseInt(m.group(2)); wp.y = Integer.parseInt(m.group(3)); wp.z = Integer.parseInt(m.group(4));
                wp.dim = dim(); wp.t = System.currentTimeMillis();
                if (Modules.WPSHARE != null && Modules.WPSHARE.s("Auto accept").on()) {
                    Config.waypoints.add(wp);
                    Notify.push("Waypoint received", wp.name + " saved", 0xFF6EE7A0);
                } else {
                    pending = wp;
                    Notify.push("Waypoint received", wp.name + " - type .wp accept", 0xFFFFD166);
                }
                return;
            }
            m = XYZ.matcher(msg);
            if (m.find() && Modules.COORDSHARE != null && Modules.COORDSHARE.enabled) {
                shared = new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
                Notify.push("Coordinates received", shared[0] + " " + shared[1] + " " + shared[2] + "  (.gps shared)", 0xFF7AA2FF);
            }
        } catch (Exception ignored) {}
    }

    // ================= FRIENDS =================
    public static Set<String> online() {
        Set<String> s = new HashSet<>();
        var h = mc().getNetworkHandler();
        if (h != null) for (var e : h.getPlayerList()) s.add(e.getProfile().getName().toLowerCase(Locale.ROOT));
        return s;
    }

    // ================= AFK =================
    public static long lastActive = System.currentTimeMillis();
    static double lx, ly, lz; static float lyaw, lpitch;

    public static void afkTick(MinecraftClient mc) {
        var p = mc.player;
        boolean moved = Math.abs(p.getX() - lx) > 0.02 || Math.abs(p.getY() - ly) > 0.02 || Math.abs(p.getZ() - lz) > 0.02
                || Math.abs(p.getYaw() - lyaw) > 0.5f || Math.abs(p.getPitch() - lpitch) > 0.5f;
        if (moved) lastActive = System.currentTimeMillis();
        lx = p.getX(); ly = p.getY(); lz = p.getZ(); lyaw = p.getYaw(); lpitch = p.getPitch();
    }

    // ================= AURA =================
    public static void aura(MinecraftClient mc, Mod m) {
        var p = mc.player;
        var w = mc.world;
        int style = m.s("Style (0-4)").i();
        double r = m.s("Radius").value;
        double t = w.getTime() * 0.25;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        switch (style) {
            case 0:
                for (int i = 0; i < 2; i++) {
                    double a = t + i * Math.PI;
                    w.addParticle(ParticleTypes.END_ROD, x + Math.cos(a) * r, y + 0.15, z + Math.sin(a) * r, 0, 0, 0);
                }
                break;
            case 1: {
                double a = t, hy = (w.getTime() % 40) / 40.0 * 1.9;
                w.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + Math.cos(a) * r, y + hy, z + Math.sin(a) * r, 0, 0, 0);
                w.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x - Math.cos(a) * r, y + 1.9 - hy, z - Math.sin(a) * r, 0, 0, 0);
                break;
            }
            case 2:
                for (int i = 0; i < 3; i++) {
                    double a = t * 1.5 + i * Math.PI * 2 / 3;
                    w.addParticle(ParticleTypes.ENCHANT, x + Math.cos(a) * r, y + 1.0 + Math.sin(t + i) * 0.5, z + Math.sin(a) * r, 0, 0, 0);
                }
                break;
            case 3:
                if (w.getTime() % 8 == 0) {
                    double a = Math.random() * Math.PI * 2;
                    w.addParticle(ParticleTypes.HEART, x + Math.cos(a) * r, y + 0.5 + Math.random() * 1.5, z + Math.sin(a) * r, 0, 0, 0);
                }
                break;
            default:
                for (int i = 0; i < 2; i++) {
                    double a = t * 2 + i * Math.PI;
                    w.addParticle(ParticleTypes.FLAME, x + Math.cos(a) * r, y + 0.1, z + Math.sin(a) * r, 0, 0.01, 0);
                }
        }
    }

    // ================= INVENTORY LAYOUTS =================
    public static void saveLayout(String name) {
        var p = mc().player;
        if (p == null) return;
        List<String> l = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            l.add(s.isEmpty() ? "" : Registries.ITEM.getId(s.getItem()).toString());
        }
        Config.layouts.put(name, l);
        Notify.push("Layout saved", name, 0xFF6EE7A0);
    }

    public static void applyLayout(String name) {
        MinecraftClient mc = mc();
        var p = mc.player;
        List<String> l = Config.layouts.get(name);
        if (p == null || l == null || mc.interactionManager == null) { Notify.push("Layout", "Nothing saved in " + name, 0xFFFF6B6B); return; }
        int moved = 0;
        for (int i = 0; i < 9 && i < l.size(); i++) {
            String want = l.get(i);
            if (want.isEmpty()) continue;
            ItemStack cur = p.getInventory().getStack(i);
            if (!cur.isEmpty() && Registries.ITEM.getId(cur.getItem()).toString().equals(want)) continue;
            for (int j = 9; j < 36; j++) {
                ItemStack s = p.getInventory().getStack(j);
                if (!s.isEmpty() && Registries.ITEM.getId(s.getItem()).toString().equals(want)) {
                    mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, j, i, SlotActionType.SWAP, p);
                    moved++;
                    break;
                }
            }
        }
        Notify.push("Layout applied", name + ": " + moved + " slots changed", 0xFF7AA2FF);
    }

    // ================= KEY CONFLICTS =================
    public static List<String> conflicts() {
        Map<Integer, List<String>> m = new LinkedHashMap<>();
        for (KeyBinding kb : mc().options.allKeys) {
            if (kb.isUnbound()) continue;
            InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(kb);
            if (k.getCategory() != InputUtil.Type.KEYSYM) continue;
            m.computeIfAbsent(k.getCode(), x -> new ArrayList<>()).add(Text.translatable(kb.getTranslationKey()).getString());
        }
        for (Mod md : Modules.ALL) if (md.key >= 0) m.computeIfAbsent(md.key, x -> new ArrayList<>()).add("TheFlai1k: " + md.name);
        List<String> out = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> e : m.entrySet())
            if (e.getValue().size() > 1) out.add("[" + Gfx.keyName(e.getKey()) + "] " + String.join("  vs  ", e.getValue()));
        return out;
    }
}
