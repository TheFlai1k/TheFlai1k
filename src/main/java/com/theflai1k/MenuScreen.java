package com.theflai1k;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MenuScreen extends Screen {
    static final int WIN = 0xD00C0E12, CARD = 0xB0181B21, CARDH = 0xC0232833, SUB = 0xFF8A93A3, TXT = 0xFFF2F4F8, EDGE = 0x2EFFFFFF, FIELD = 0xFF0A0C10;
    static final int CH = 34;
    static final int[] ROW = {0};

    static final class Btn { int x, y, w, h; Runnable l, r; }
    static final class Sl { Setting s; int x, y, w; }

    int tab, page, wx, wy, ww, wh, bx, bw, gx, gy, gw, gh, clipTop, maxScroll, galPage, sortMode;
    final long t0 = System.currentTimeMillis();
    float fade, scroll, scrollT;
    Mod.Cat filter;
    Mod open, binding;
    boolean clip, confDirty = true;
    String focus = "", status = "", lastQ = null;
    final Map<String, StringBuilder> inputs = new HashMap<>();
    final List<Btn> btns = new ArrayList<>();
    final List<Sl> sls = new ArrayList<>();
    Sl drag;
    List<Tools.BlockInfo> results = new ArrayList<>();
    Tools.BlockInfo prev;
    List<String> conflicts = new ArrayList<>();

    public MenuScreen() { this(0, 0); }

    public MenuScreen(int tab, int page) {
        super(Text.literal("TheFlai1k"));
        setTab(tab, page);
    }

    void setTab(int t, int p) {
        tab = t; page = p; open = null; binding = null; scroll = scrollT = 0; status = "";
        focus = t == 0 ? "search" : (t == 1 && p == 0) ? "bs" : t == 4 ? "notes" : "";
        if (t == 2) { Tools.refreshShots(); galPage = 0; }
        if (t == 5) confDirty = true;
    }

    @Override public boolean shouldPause() { return false; }

    // ---------- helpers ----------
    int A(int c) { return Gfx.a(c, fade); }
    boolean in(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    StringBuilder sb(String k) { return inputs.computeIfAbsent(k, x -> new StringBuilder()); }
    String val(String k) { StringBuilder b = inputs.get(k); return b == null ? "" : b.toString(); }

    void layout() {
        ww = Math.min(width - 16, 580);
        wh = Math.min(height - 16, 340);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2 + (int) ((1 - fade) * 10);
        bx = wx + 36; bw = ww - 36;
        gx = bx + 12; gy = wy + 62; gw = bw - 24; gh = wh - 62 - 10;
    }

    void reg(int x, int y, int w, int h, Runnable l, Runnable r) {
        if (clip && (y + h < clipTop || y > gy + gh)) return;
        Btn b = new Btn();
        b.x = x; b.y = y; b.w = w; b.h = h; b.l = l; b.r = r;
        btns.add(b);
    }

    void beginScroll(DrawContext c, int top) {
        clipTop = top; clip = true;
        c.enableScissor(gx, top, gx + gw, gy + gh);
    }

    void endScroll(DrawContext c, int contentH, int top) {
        c.disableScissor();
        clip = false;
        maxScroll = Math.max(0, contentH - (gy + gh - top));
        scrollT = MathHelper.clamp(scrollT, 0, maxScroll);
    }

    void header(DrawContext c, String t, String s) {
        Gfx.bold(c, t, bx + 12, wy + 10, A(TXT));
        Gfx.text(c, s, bx + 12, wy + 22, A(SUB));
    }

    void btn(DrawContext c, String label, int x, int y, int w, int h, Runnable l, int mx, int my) {
        boolean hov = in(mx, my, x, y, w, h) && (!clip || (my >= clipTop && my < gy + gh));
        Gfx.rrect(c, x, y, w, h, 5, A(hov ? 0x45FFFFFF : 0x26FFFFFF));
        String s = Gfx.fit(label, w - 6);
        Gfx.text(c, s, x + (w - Gfx.w(s)) / 2, y + (h - 8) / 2, A(TXT));
        reg(x, y, w, h, l, null);
    }

    void field(DrawContext c, String key, int x, int y, int w, String ph) {
        boolean f = focus.equals(key);
        Gfx.rrect(c, x - 1, y - 1, w + 2, 18, 5, A(f ? Gfx.a(Config.accent(), 0.6f) : EDGE));
        Gfx.rrect(c, x, y, w, 16, 4, A(FIELD));
        String v = val(key);
        boolean empty = v.isEmpty() && !f;
        String shown = empty ? ph : v + (f && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        Gfx.text(c, Gfx.fitEnd(shown, w - 10), x + 5, y + 4, A(empty ? SUB : TXT));
        reg(x, y, w, 16, () -> focus = key, null);
    }

    void row(DrawContext c, String name, int x, int y, int w) {
        Gfx.rrect(c, x, y, w, 22, 6, A(CARD));
        Gfx.text(c, Gfx.fit(name, w - 150), x + 10, y + 7, A(TXT));
    }

    void slider(DrawContext c, int x, int y, int w, Setting s) {
        Gfx.rrect(c, x, y, w, 4, 2, A(0xFF2E333B));
        double t = (s.value - s.min) / (s.max - s.min);
        Gfx.rrect(c, x, y, Math.max(4, (int) (w * t)), 4, 2, A(Config.accent()));
        Gfx.rrect(c, x + (int) (w * t) - 4, y - 3, 8, 10, 4, A(0xFFFFFFFF));
        if (!clip || (y + 8 > clipTop && y < gy + gh)) {
            Sl sl = new Sl(); sl.s = s; sl.x = x; sl.y = y; sl.w = w; sls.add(sl);
        }
    }

    void toggleRow(DrawContext c, String name, int x, int y, int w, Setting s) {
        row(c, name, x, y, w);
        Gfx.toggle(c, x + w - 32, y + 5, (float) s.value, fade);
        reg(x, y, w, 22, () -> s.value = s.on() ? 0 : 1, null);
    }

    void sliderRow(DrawContext c, int x, int y, int w, Setting s) {
        row(c, s.name, x, y, w);
        int tw = Math.min(130, w / 3), tx = x + w - 12 - tw;
        String d = s.display();
        Gfx.text(c, d, tx - 8 - Gfx.w(d), y + 7, A(SUB));
        slider(c, tx, y + 9, tw, s);
    }

    void pills(DrawContext c, int mx, int my, String[] lab, int sel, IntConsumer pick) {
        int x = gx, y = wy + 40;
        for (int i = 0; i < lab.length; i++) {
            int w = Gfx.w(lab[i]) + 14;
            boolean s = i == sel, hov = in(mx, my, x, y, w, 14);
            Gfx.rrect(c, x, y, w, 14, 7, A(s ? 0xFFF2F4F8 : hov ? 0x40FFFFFF : 0x22FFFFFF));
            Gfx.text(c, lab[i], x + 7, y + 3, A(s ? 0xFF0C0E12 : TXT));
            final int idx = i;
            reg(x, y, w, 14, () -> pick.accept(idx), null);
            x += w + 4;
        }
    }

    // ---------- main render ----------
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        fade = Math.min(1f, (System.currentTimeMillis() - t0) / 180f);
        layout();
        btns.clear(); sls.clear(); clip = false;
        scroll += (scrollT - scroll) * 0.3f;
        if (Math.abs(scrollT - scroll) < 0.3f) scroll = scrollT;
        side(c, mx, my);
        Gfx.rrect(c, bx - 1, wy - 1, bw + 2, wh + 2, 9, A(EDGE));
        Gfx.rrect(c, bx, wy, bw, wh, 8, A(WIN));
        switch (tab) {
            case 0: pageMods(c, mx, my); break;
            case 1: pageTools(c, mx, my); break;
            case 2: pageGallery(c, mx, my); break;
            case 3: pageSocial(c, mx, my); break;
            case 4: pageNotes(c, mx, my); break;
            default: pageSettings(c, mx, my);
        }
        if (!status.isEmpty()) Gfx.text(c, Gfx.fit(status, bw - 24), bx + 12, wy + wh - 10, A(Config.accent()));
    }

    // ---------- sidebar ----------
    void side(DrawContext c, int mx, int my) {
        Gfx.rrect(c, wx - 1, wy - 1, 32, wh + 2, 9, A(EDGE));
        Gfx.rrect(c, wx, wy, 30, wh, 8, A(WIN));
        Gfx.rrect(c, wx + 4, wy + 8, 22, 22, 11, A(0xFFFFFFFF));
        Gfx.bold(c, "F", wx + 15 - Gfx.w("F") / 2 - 1, wy + 15, A(0xFF0C0E12));
        for (int i = 0; i < 7; i++) {
            int ty = wy + 40 + i * 26;
            boolean sel = i == tab, hov = in(mx, my, wx + 4, ty, 22, 22);
            if (sel || hov) Gfx.rrect(c, wx + 4, ty, 22, 22, 6, A(sel ? 0x30FFFFFF : 0x16FFFFFF));
            icon(c, i, wx + 15, ty + 11, A(sel ? TXT : SUB));
            final int idx = i;
            reg(wx + 4, ty, 22, 22, () -> {
                if (idx == 6) { if (client != null) client.setScreen(new HudEditScreen()); }
                else setTab(idx, 0);
            }, null);
        }
    }

    void icon(DrawContext c, int i, int cx, int cy, int col) {
        switch (i) {
            case 0:
                c.fill(cx - 5, cy - 5, cx - 1, cy - 1, col); c.fill(cx + 1, cy - 5, cx + 5, cy - 1, col);
                c.fill(cx - 5, cy + 1, cx - 1, cy + 5, col); c.fill(cx + 1, cy + 1, cx + 5, cy + 5, col);
                break;
            case 1:
                Gfx.outline(c, cx - 5, cy - 3, 10, 8, col); c.fill(cx - 5, cy - 6, cx + 1, cy - 3, col);
                break;
            case 2:
                Gfx.outline(c, cx - 6, cy - 5, 12, 10, col); c.fill(cx - 4, cy + 1, cx - 1, cy + 3, col); c.fill(cx, cy - 1, cx + 4, cy + 3, col);
                break;
            case 3:
                Gfx.rrect(c, cx - 5, cy - 5, 5, 5, 2, col); Gfx.rrect(c, cx + 1, cy - 5, 5, 5, 2, col);
                Gfx.rrect(c, cx - 6, cy + 1, 7, 5, 2, col); Gfx.rrect(c, cx + 1, cy + 1, 6, 5, 2, col);
                break;
            case 4:
                c.fill(cx - 5, cy - 4, cx + 5, cy - 3, col); c.fill(cx - 5, cy, cx + 5, cy + 1, col); c.fill(cx - 5, cy + 4, cx + 2, cy + 5, col);
                break;
            case 5:
                Gfx.rrect(c, cx - 5, cy - 5, 10, 10, 5, col); Gfx.rrect(c, cx - 2, cy - 2, 4, 4, 2, A(WIN));
                c.fill(cx - 1, cy - 7, cx + 1, cy - 5, col); c.fill(cx - 1, cy + 5, cx + 1, cy + 7, col);
                c.fill(cx - 7, cy - 1, cx - 5, cy + 1, col); c.fill(cx + 5, cy - 1, cx + 7, cy + 1, col);
                break;
            default:
                c.fill(cx - 1, cy - 6, cx + 1, cy + 6, col); c.fill(cx - 6, cy - 1, cx + 6, cy + 1, col);
        }
    }

    // ---------- MODS ----------
    List<Mod> list() {
        List<Mod> r = new ArrayList<>();
        String q = val("search").toLowerCase(Locale.ROOT).trim();
        for (Mod m : Modules.ALL) {
            if (filter != null && m.cat != filter) continue;
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q) && !m.desc.toLowerCase(Locale.ROOT).contains(q)) continue;
            r.add(m);
        }
        return r;
    }

    void pageMods(DrawContext c, int mx, int my) {
        if (open != null) { modSettings(c, mx, my); return; }
        int en = 0, tot = 0;
        for (Mod m : Modules.ALL) if (!m.isTool()) { tot++; if (m.enabled) en++; }
        header(c, "Mods", en + " of " + tot + " enabled");
        field(c, "search", bx + bw - 12 - 150, wy + 10, 150, "Search name or description...");
        Mod.Cat[] cs = Mod.Cat.values();
        String[] lab = new String[cs.length + 1];
        lab[0] = "All";
        for (int i = 0; i < cs.length; i++) lab[i + 1] = cs[i].label;
        pills(c, mx, my, lab, filter == null ? 0 : filter.ordinal() + 1, i -> { filter = i == 0 ? null : cs[i - 1]; scrollT = 0; });
        List<Mod> L = list();
        int cols = gw >= 400 ? 3 : 2, cw = (gw - (cols - 1) * 6) / cols;
        int rows = (L.size() + cols - 1) / cols;
        beginScroll(c, gy);
        int oy = gy - Math.round(scroll);
        for (int i = 0; i < L.size(); i++) {
            int x = gx + (i % cols) * (cw + 6), y = oy + (i / cols) * (CH + 6);
            if (y + CH < gy || y > gy + gh) continue;
            card(c, L.get(i), x, y, cw, mx, my);
        }
        endScroll(c, rows * (CH + 6), gy);
    }

    void card(DrawContext c, Mod m, int x, int y, int w, int mx, int my) {
        boolean tool = m.isTool();
        boolean hov = in(mx, my, x, y, w, CH) && my >= gy && my < gy + gh;
        m.anim += ((m.enabled && !tool ? 1 : 0) - m.anim) * 0.25f;
        if (m.anim > 0.4f) Gfx.rrect(c, x - 1, y - 1, w + 2, CH + 2, 7, A(Gfx.a(Config.accent(), 0.4f)));
        Gfx.rrect(c, x, y, w, CH, 6, A(hov ? CARDH : CARD));
        Gfx.rrect(c, x + 6, y + 6, 22, 22, 5, A(Gfx.mix(0x30FFFFFF, Gfx.a(Config.accent(), 0.35f), m.anim)));
        String l = m.name.substring(0, 1);
        Gfx.bold(c, l, x + 17 - Gfx.w(l) / 2, y + 13, A(m.anim > 0.5f ? Config.accent() : SUB));
        String nm = Gfx.fit(m.name, w - 34 - 54);
        Gfx.text(c, nm, x + 34, y + 7, A(TXT));
        if (m.key >= 0) Gfx.text(c, "[" + Gfx.keyName(m.key) + "]", x + 36 + Gfx.w(nm), y + 7, A(Config.accent()));
        String d = binding == m ? "Press a key... (Esc = clear)" : m.desc;
        Gfx.text(c, Gfx.fit(d, w - 34 - 54), x + 34, y + 19, A(binding == m ? Config.accent() : SUB));
        if (tool) Gfx.text(c, "Open >", x + w - 8 - Gfx.w("Open >"), y + 13, A(SUB));
        else Gfx.toggle(c, x + w - 30, y + (CH - 11) / 2, m.anim, fade);
        int gxp = x + w - (tool ? 58 : 46);
        for (int k = 0; k < 3; k++) c.fill(gxp, y + 12 + k * 3, gxp + 9, y + 13 + k * 3, A(SUB));
        reg(x, y, w, CH, () -> { if (tool) setTab(m.tab, m.page); else m.setEnabled(!m.enabled); }, () -> binding = m);
        reg(gxp - 4, y, 18, CH, () -> { open = m; scroll = scrollT = 0; }, () -> binding = m);
    }

    void modSettings(DrawContext c, int mx, int my) {
        header(c, open.name, open.desc);
        btn(c, "< Back", bx + bw - 72, wy + 10, 60, 16, () -> { open = null; scroll = scrollT = 0; }, mx, my);
        beginScroll(c, gy);
        int oy = gy - Math.round(scroll), i = 0;
        for (Setting s : open.settings) {
            int y = oy + i * 26;
            if (s.bool) toggleRow(c, s.name, gx, y, gw, s); else sliderRow(c, gx, y, gw, s);
            i++;
        }
        for (Object[] a : open.actions) {
            int y = oy + i * 26;
            row(c, (String) a[0], gx, y, gw);
            @SuppressWarnings("unchecked") Consumer<net.minecraft.client.MinecraftClient> act = (Consumer<net.minecraft.client.MinecraftClient>) a[1];
            btn(c, "Run", gx + gw - 58, y + 3, 50, 16, () -> act.accept(client), mx, my);
            i++;
        }
        int y = oy + i * 26;
        row(c, "Keybind", gx, y, gw);
        String kl = binding == open ? "Press a key..." : open.key < 0 ? "None" : Gfx.keyName(open.key) + (open.hold ? " (hold)" : "");
        btn(c, kl, gx + gw - 118, y + 3, 110, 16, () -> binding = open, mx, my);
        i++;
        if (!open.isTool() && !open.hud) {
            y = oy + i * 26;
            row(c, "Pin to Radial Menu", gx, y, gw);
            Gfx.toggle(c, gx + gw - 32, y + 5, open.pinned ? 1 : 0, fade);
            reg(gx, y, gw, 22, () -> open.pinned = !open.pinned, null);
            i++;
        }
        endScroll(c, i * 26, gy);
    }

    // ---------- TOOLS ----------
    void pageTools(DrawContext c, int mx, int my) {
        header(c, "Tools", page == 0 ? "Block search and preview" : page == 1 ? "Build palettes" : "Color picker");
        pills(c, mx, my, new String[]{"Block Search", "Palettes", "Color Picker"}, page, i -> { page = i; scroll = scrollT = 0; focus = i == 0 ? "bs" : i == 1 ? "pname" : ""; });
        if (page == 0) blockSearch(c, mx, my);
        else if (page == 1) palettes(c, mx, my);
        else colorPage(c, mx, my);
    }

    void blockSearch(DrawContext c, int mx, int my) {
        int lw = gw / 2 - 4, rx = gx + lw + 8, rw = gw - lw - 8;
        field(c, "bs", gx, gy, lw, "Search blocks...");
        String q = val("bs").toLowerCase(Locale.ROOT).trim();
        if (!q.equals(lastQ)) {
            lastQ = q;
            results = new ArrayList<>();
            for (Tools.BlockInfo b : Tools.blocks()) {
                if (q.isEmpty() || b.name.toLowerCase(Locale.ROOT).contains(q) || b.id.contains(q)) { results.add(b); if (results.size() >= 400) break; }
            }
            scroll = scrollT = 0;
        }
        int top = gy + 22;
        beginScroll(c, top);
        int oy = top - Math.round(scroll);
        for (int i = 0; i < results.size(); i++) {
            int y = oy + i * 20;
            if (y + 20 < top || y > gy + gh) continue;
            Tools.BlockInfo b = results.get(i);
            boolean hov = in(mx, my, gx, y, lw, 18) && my >= top && my < gy + gh;
            Gfx.rrect(c, gx, y, lw, 18, 5, A(b == prev ? 0x40FFFFFF : hov ? CARDH : CARD));
            c.drawItem(b.st, gx + 2, y + 1);
            Gfx.text(c, Gfx.fit(b.name, lw - 50), gx + 22, y + 5, A(TXT));
            reg(gx, y, lw, 18, () -> prev = b, null);
            btn(c, "+", gx + lw - 22, y + 1, 18, 16, () -> Tools.addToPalette(b), mx, my);
        }
        endScroll(c, results.size() * 20, top);
        // preview window
        Gfx.rrect(c, rx, gy, rw, gh, 6, A(CARD));
        if (prev == null) { Gfx.text(c, "Click a block to preview it", rx + 10, gy + 10, A(SUB)); return; }
        int sc = rw >= 120 && gh >= 150 ? 4 : 2;
        var mt = c.getMatrices();
        mt.pushMatrix();
        mt.translate(rx + rw / 2f - 8f * sc, gy + 10f);
        mt.scale((float) sc, (float) sc);
        c.drawItem(prev.st, 0, 0);
        mt.popMatrix();
        int ty = gy + 16 * sc + 16;
        Gfx.bold(c, Gfx.fit(prev.name, rw - 16), rx + 8, ty, A(TXT));
        var st = prev.b.getDefaultState();
        String[] info = {"id: " + prev.id,
                String.format(Locale.ROOT, "Hardness %.1f  Blast %.1f", prev.b.getHardness(), prev.b.getBlastResistance()),
                "Light level: " + st.getLuminance()};
        for (int i = 0; i < info.length; i++) Gfx.text(c, Gfx.fit(info[i], rw - 16), rx + 8, ty + 12 + i * 10, A(SUB));
        int cy = ty + 12 + info.length * 10 + 2;
        if (prev.color != 0) {
            Gfx.rrect(c, rx + 8, cy, 14, 10, 3, A(0xFF000000 | prev.color));
            Gfx.text(c, String.format("#%06X", prev.color & 0xFFFFFF), rx + 26, cy + 1, A(SUB));
        }
        int by = gy + gh - 40;
        if (by > cy + 10) {
            btn(c, "Add to '" + Tools.cur + "'", rx + 8, by, rw - 16, 16, () -> Tools.addToPalette(prev), mx, my);
            btn(c, "Copy id", rx + 8, by + 20, rw - 16, 16, () -> { Tools.clip(prev.id); status = "Copied: " + prev.id; }, mx, my);
        }
    }

    void palettes(DrawContext c, int mx, int my) {
        Tools.pal(Tools.cur);
        int w2 = gw - 80;
        field(c, "pname", gx, gy, w2, "New palette name...");
        btn(c, "Create", gx + w2 + 6, gy, gw - w2 - 6, 16, () -> {
            String n = val("pname").trim();
            if (!n.isEmpty()) { Tools.pal(n); Tools.cur = n; sb("pname").setLength(0); Config.save(); }
        }, mx, my);
        int x = gx, y = gy + 22;
        for (String n : new ArrayList<>(Config.palettes.keySet())) {
            int w = Gfx.w(n) + 14;
            if (x + w > gx + gw) { x = gx; y += 18; }
            boolean sel = n.equals(Tools.cur);
            Gfx.rrect(c, x, y, w, 14, 7, A(sel ? 0xFFF2F4F8 : 0x26FFFFFF));
            Gfx.text(c, n, x + 7, y + 3, A(sel ? 0xFF0C0E12 : TXT));
            final String nn = n;
            reg(x, y, w, 14, () -> Tools.cur = nn, null);
            x += w + 4;
        }
        y += 20;
        btn(c, "Copy list", gx, y, 70, 16, () -> { Tools.clip(String.join(", ", Tools.pal(Tools.cur))); status = "Palette copied"; }, mx, my);
        btn(c, "Clear", gx + 74, y, 56, 16, () -> { Tools.pal(Tools.cur).clear(); Config.save(); }, mx, my);
        btn(c, "Delete palette", gx + 134, y, 90, 16, () -> {
            Config.palettes.remove(Tools.cur);
            Tools.cur = Config.palettes.isEmpty() ? "Default" : Config.palettes.keySet().iterator().next();
            Config.save();
        }, mx, my);
        List<String> ids = new ArrayList<>(Tools.pal(Tools.cur));
        Gfx.text(c, ids.size() + " blocks - click a block to remove it", gx, y + 22, A(SUB));
        int top = y + 34;
        int cols = Math.max(1, gw / 28);
        beginScroll(c, top);
        int oy = top - Math.round(scroll);
        String hover = null;
        for (int i = 0; i < ids.size(); i++) {
            int cx2 = gx + (i % cols) * 28, cy2 = oy + (i / cols) * 28;
            if (cy2 + 26 < top || cy2 > gy + gh) continue;
            Tools.BlockInfo b = Tools.findBlock(ids.get(i));
            boolean hov = in(mx, my, cx2, cy2, 26, 26) && my >= top && my < gy + gh;
            Gfx.rrect(c, cx2, cy2, 26, 26, 5, A(hov ? CARDH : CARD));
            if (b != null) { c.drawItem(b.st, cx2 + 5, cy2 + 5); if (hov) hover = b.name; }
            final String id = ids.get(i);
            reg(cx2, cy2, 26, 26, () -> { Tools.pal(Tools.cur).remove(id); Config.save(); }, null);
        }
        endScroll(c, ((ids.size() + cols - 1) / cols) * 28, top);
        if (hover != null) {
            int hw = Gfx.w(hover) + 10;
            Gfx.rrect(c, mx + 8, my - 4, hw, 14, 4, 0xF0000000);
            Gfx.text(c, hover, mx + 13, my - 1, 0xFFFFFFFF);
        }
    }

    void colorPage(DrawContext c, int mx, int my) {
        int lw = gw / 2 - 4, rx = gx + lw + 8, rw = gw - lw - 8;
        beginScroll(c, gy);
        int oy = gy - Math.round(scroll);
        sliderRow(c, gx, oy, lw, Tools.CR);
        sliderRow(c, gx, oy + 26, lw, Tools.CG);
        sliderRow(c, gx, oy + 52, lw, Tools.CB);
        int rgb = Tools.rgb();
        Gfx.rrect(c, gx, oy + 82, lw, 34, 6, 0xFF000000 | rgb);
        Gfx.text(c, String.format("#%06X", rgb), gx + 8, oy + 95, (((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255)) / 3 > 128 ? 0xFF000000 : 0xFFFFFFFF);
        btn(c, "Copy hex", gx, oy + 120, lw, 16, () -> { Tools.clip(String.format("#%06X", Tools.rgb())); status = "Hex copied"; }, mx, my);
        Gfx.text(c, "Harmony (click to use):", gx, oy + 144, A(SUB));
        float[] shifts = {180f, 30f, -30f, 120f, -120f};
        for (int i = 0; i < shifts.length; i++) {
            int col = Tools.hueShift(rgb, shifts[i]);
            int sx = gx + i * 30;
            Gfx.rrect(c, sx, oy + 156, 26, 18, 4, 0xFF000000 | col);
            reg(sx, oy + 156, 26, 18, () -> Tools.setRgb(col), null);
        }
        Gfx.rrect(c, rx, oy, rw, 12 + 22 * 10, 6, A(CARD));
        Gfx.text(c, "Closest blocks", rx + 8, oy + 5, A(SUB));
        List<Tools.BlockInfo> near = Tools.nearest(rgb);
        for (int i = 0; i < near.size(); i++) {
            int y = oy + 16 + i * 22;
            Tools.BlockInfo b = near.get(i);
            c.drawItem(b.st, rx + 6, y);
            Gfx.rrect(c, rx + 24, y + 3, 8, 10, 2, 0xFF000000 | b.color);
            Gfx.text(c, Gfx.fit(b.name, rw - 24 - 40), rx + 36, y + 4, A(TXT));
            btn(c, "+", rx + rw - 24, y, 18, 16, () -> Tools.addToPalette(b), mx, my);
        }
        endScroll(c, 12 + 22 * 10 + 4, gy);
    }

    // ---------- GALLERY ----------
    void pageGallery(DrawContext c, int mx, int my) {
        header(c, "Gallery", "Screenshots, editor and blueprints (F2 in game takes a screenshot)");
        btn(c, "Refresh", bx + bw - 12 - 60, wy + 10, 60, 16, () -> { Tools.refreshShots(); status = "Refreshed"; }, mx, my);
        btn(c, "Folder", bx + bw - 12 - 60 - 64, wy + 10, 60, 16, () -> Tools.openFile(Tools.shotDir()), mx, my);
        List<File> S = Tools.shots;
        int lw = Math.min(150, gw / 3), rx = gx + lw + 8, rw = gw - lw - 8;
        beginScroll(c, gy);
        int oy = gy - Math.round(scroll);
        int per = 10, pages = Math.max(1, (S.size() + per - 1) / per);
        galPage = MathHelper.clamp(galPage, 0, pages - 1);
        for (int i = 0; i < per; i++) {
            int idx = galPage * per + i;
            if (idx >= S.size()) break;
            File f = S.get(idx);
            int y = oy + i * 16;
            boolean hov = in(mx, my, gx, y, lw, 14) && my >= gy && my < gy + gh;
            Gfx.rrect(c, gx, y, lw, 14, 4, A(f.equals(Tools.selFile) ? 0x40FFFFFF : hov ? CARDH : CARD));
            Gfx.text(c, Gfx.fit(f.getName().replace(".png", ""), lw - 8), gx + 4, y + 3, A(TXT));
            reg(gx, y, lw, 14, () -> Tools.select(f), null);
        }
        int py2 = oy + per * 16 + 2;
        btn(c, "<", gx, py2, 24, 14, () -> galPage--, mx, my);
        String pt = (galPage + 1) + "/" + pages;
        Gfx.text(c, pt, gx + lw / 2 - Gfx.w(pt) / 2, py2 + 3, A(SUB));
        btn(c, ">", gx + lw - 24, py2, 24, 14, () -> galPage++, mx, my);
        int contentH = per * 16 + 20;
        if (S.isEmpty()) {
            Gfx.text(c, "No screenshots yet. Press F2 in game.", rx, oy + 4, A(SUB));
        } else if (Tools.selFile == null) {
            Gfx.text(c, "Select a screenshot on the left", rx, oy + 4, A(SUB));
        } else {
            int cell = rw >= 200 ? 2 : 1, pw = Tools.TW * cell, ph = Tools.TH * cell;
            int[] th = Tools.thumb;
            Gfx.rrect(c, rx - 2, oy - 2, pw + 4, ph + 4, 4, A(CARD));
            if (th != null) {
                int cl = Tools.CL.i(), cr = Tools.CRT.i(), ct = Tools.CT.i(), cb = Tools.CB2.i(), br = Tools.BR.i() * 2;
                boolean gray = Tools.GRAY.on();
                for (int yy = 0; yy < Tools.TH; yy++) {
                    int sy = oy + yy * cell;
                    if (sy + cell < gy || sy > gy + gh) continue;
                    for (int xx = 0; xx < Tools.TW; xx++) {
                        int p = th[yy * Tools.TW + xx];
                        int r = Tools.cl(((p >> 16) & 255) + br), g = Tools.cl(((p >> 8) & 255) + br), b = Tools.cl((p & 255) + br);
                        if (gray) { int v = (r + g + b) / 3; r = g = b = v; }
                        boolean out = xx * 100 / Tools.TW < cl || xx * 100 / Tools.TW >= 100 - cr || yy * 100 / Tools.TH < ct || yy * 100 / Tools.TH >= 100 - cb;
                        if (out) { r /= 4; g /= 4; b /= 4; }
                        c.fill(rx + xx * cell, sy, rx + xx * cell + cell, sy + cell, 0xFF000000 | (r << 16) | (g << 8) | b);
                    }
                }
            } else Gfx.text(c, "Cannot read this image", rx + 6, oy + 6, A(SUB));
            int y = oy + ph + 8;
            sliderRow(c, rx, y, rw, Tools.CL); y += 24;
            sliderRow(c, rx, y, rw, Tools.CRT); y += 24;
            sliderRow(c, rx, y, rw, Tools.CT); y += 24;
            sliderRow(c, rx, y, rw, Tools.CB2); y += 24;
            sliderRow(c, rx, y, rw, Tools.BR); y += 24;
            toggleRow(c, "Grayscale", rx, y, rw, Tools.GRAY); y += 26;
            int bw2 = (rw - 6) / 2;
            btn(c, "Save edited copy", rx, y, bw2, 16, () -> status = Tools.saveEdited(), mx, my);
            btn(c, "Make blueprint", rx + bw2 + 6, y, bw2, 16, () -> status = Tools.blueprint(), mx, my);
            y += 20;
            btn(c, "Open file", rx, y, bw2, 16, () -> Tools.openFile(Tools.selFile), mx, my);
            btn(c, "Copy path", rx + bw2 + 6, y, bw2, 16, () -> { Tools.clip(Tools.selFile.getAbsolutePath()); status = "Path copied - paste it into Discord / chat"; }, mx, my);
            contentH = Math.max(contentH, y + 24 - oy);
        }
        endScroll(c, contentH, gy);
    }

    // ---------- SOCIAL ----------
    static final String[] SORT = {"Name", "Distance", "Newest"};

    void pageSocial(DrawContext c, int mx, int my) {
        header(c, "Social", page == 0 ? "Your friend list" : "Waypoints and coordinate sharing");
        pills(c, mx, my, new String[]{"Friends", "Waypoints"}, page, i -> { page = i; scroll = scrollT = 0; focus = i == 0 ? "fname" : "wpname"; });
        if (page == 0) friends(c, mx, my); else waypoints(c, mx, my);
    }

    void friends(DrawContext c, int mx, int my) {
        int w2 = gw - 70;
        field(c, "fname", gx, gy, w2, "Friend nickname...");
        btn(c, "Add", gx + w2 + 6, gy, gw - w2 - 6, 16, () -> {
            String n = val("fname").trim();
            if (!n.isEmpty() && !Modules.isFriend(n)) { Config.friends.add(n); Config.save(); }
            sb("fname").setLength(0);
        }, mx, my);
        Set<String> on = Tools.online();
        int top = gy + 22;
        beginScroll(c, top);
        int oy = top - Math.round(scroll);
        List<String> fl = new ArrayList<>(Config.friends);
        for (int i = 0; i < fl.size(); i++) {
            int y = oy + i * 24;
            String n = fl.get(i);
            row(c, n, gx, y, gw);
            boolean o = on.contains(n.toLowerCase(Locale.ROOT));
            Gfx.text(c, o ? "online" : "offline", gx + gw - 120, y + 7, A(o ? 0xFF6EE7A0 : SUB));
            btn(c, "Remove", gx + gw - 62, y + 3, 54, 16, () -> { Config.friends.remove(n); Config.save(); }, mx, my);
        }
        if (fl.isEmpty()) Gfx.text(c, "No friends yet. Add a nickname above (or use .friend add <name>).", gx, oy + 4, A(SUB));
        endScroll(c, Math.max(fl.size() * 24, 20), top);
    }

    void waypoints(DrawContext c, int mx, int my) {
        int w2 = gw - 190;
        field(c, "wpname", gx, gy, w2, "Waypoint name...");
        btn(c, "Add here", gx + w2 + 6, gy, 64, 16, () -> {
            var p = client.player;
            if (p == null) return;
            String n = val("wpname").trim().replace(' ', '_');
            if (n.isEmpty()) n = "WP" + (Config.waypoints.size() + 1);
            Tools.addWaypoint(n, p.getBlockX(), p.getBlockY(), p.getBlockZ());
            sb("wpname").setLength(0);
            Config.save();
        }, mx, my);
        btn(c, "Sort: " + SORT[sortMode], gx + w2 + 74, gy, gw - w2 - 74, 16, () -> sortMode = (sortMode + 1) % 3, mx, my);
        btn(c, "Send my coords", gx, gy + 20, 96, 16, Tools::sendCoords, mx, my);
        btn(c, "Copy coords", gx + 100, gy + 20, 80, 16, Tools::copyCoords, mx, my);
        if (Tools.pending != null)
            btn(c, "Accept '" + Tools.pending.name + "'", gx + 184, gy + 20, Math.min(150, gw - 184), 16, () -> { Tools.acceptPending(); Config.save(); }, mx, my);
        int top = gy + 42;
        beginScroll(c, top);
        int oy = top - Math.round(scroll);
        List<Config.Waypoint> ws = new ArrayList<>(Config.waypoints);
        if (sortMode == 0) ws.sort(Comparator.comparing((Config.Waypoint w) -> w.name.toLowerCase(Locale.ROOT)));
        else if (sortMode == 1) ws.sort(Comparator.comparingDouble(Tools::dist));
        else ws.sort((a, b) -> Long.compare(b.t, a.t));
        for (int i = 0; i < ws.size(); i++) {
            Config.Waypoint w = ws.get(i);
            int y = oy + i * 24;
            Gfx.rrect(c, gx, y, gw, 22, 6, A(CARD));
            String t = w.name + "  " + w.x + " " + w.y + " " + w.z + "  [" + w.dim + "]  " + Modules.dist(Tools.dist(w));
            Gfx.text(c, Gfx.fit(t, gw - 140), gx + 8, y + 7, A(TXT));
            btn(c, "GPS", gx + gw - 128, y + 3, 32, 16, () -> { Modules.gps = new double[]{w.x, w.y, w.z}; status = "GPS -> " + w.name; }, mx, my);
            btn(c, "Share", gx + gw - 92, y + 3, 40, 16, () -> Tools.shareWp(w), mx, my);
            btn(c, "Del", gx + gw - 48, y + 3, 40, 16, () -> { Config.waypoints.remove(w); Config.save(); }, mx, my);
        }
        if (ws.isEmpty()) Gfx.text(c, "No waypoints. Add one above or use .wp add <name>.", gx, oy + 4, A(SUB));
        endScroll(c, Math.max(ws.size() * 24, 20), top);
    }

    // ---------- NOTES ----------
    List<String> wrap(String s, int w) {
        List<String> out = new ArrayList<>();
        for (String line : s.split("\n", -1)) {
            StringBuilder cur = new StringBuilder();
            for (char ch : line.toCharArray()) {
                if (Gfx.w(cur.toString() + ch) > w) { out.add(cur.toString()); cur = new StringBuilder(); }
                cur.append(ch);
            }
            out.add(cur.toString());
        }
        return out;
    }

    void pageNotes(DrawContext c, int mx, int my) {
        header(c, "Notes", "Your in-game notepad, saved automatically");
        int ay = wy + 40, ah = wh - 40 - 22;
        Gfx.rrect(c, gx, ay, gw, ah, 6, A(FIELD));
        boolean blink = focus.equals("notes") && (System.currentTimeMillis() / 500) % 2 == 0;
        List<String> ls = wrap(Config.notes + (blink ? "_" : ""), gw - 16);
        int maxL = Math.max(1, (ah - 10) / 10), start = Math.max(0, ls.size() - maxL);
        for (int i = start; i < ls.size(); i++) Gfx.text(c, ls.get(i), gx + 8, ay + 6 + (i - start) * 10, A(TXT));
        if (Config.notes.isEmpty() && !focus.equals("notes")) Gfx.text(c, "Click here and type...", gx + 8, ay + 6, A(SUB));
        reg(gx, ay, gw, ah, () -> focus = "notes", null);
    }

    // ---------- SETTINGS ----------
    void pageSettings(DrawContext c, int mx, int my) {
        header(c, "Settings", "Appearance, notifications and key conflicts");
        if (confDirty) { conflicts = Tools.conflicts(); confDirty = false; }
        beginScroll(c, wy + 40);
        int oy = wy + 40 - Math.round(scroll), y = oy;
        row(c, "Accent color", gx, y, gw);
        for (int i = 0; i < Config.ACC.length; i++) {
            int sx = gx + gw - 8 - (Config.ACC.length - i) * 20;
            Gfx.rrect(c, sx, y + 4, 14, 14, 4, A(Config.ACC[i]));
            if (i == Config.accentIdx) Gfx.outline(c, sx - 1, y + 3, 16, 16, A(0xFFFFFFFF));
            final int k = i;
            reg(sx, y + 4, 14, 14, () -> Config.accentIdx = k, null);
        }
        y += 26;
        sliderRow(c, gx, y, gw, Config.OPACITY); y += 26;
        row(c, "HUD panels", gx, y, gw);
        btn(c, "Reset positions", gx + gw - 108, y + 3, 100, 16, () -> { for (Mod m : Modules.ALL) if (m.hud && !m.full) { m.x = m.defX; m.y = m.defY; } status = "HUD positions reset"; }, mx, my);
        y += 26;
        row(c, "Menu key", gx, y, gw);
        Gfx.text(c, Gfx.keyName(KeyBindingHelper.getBoundKeyOf(TheFlai1k.menuKey).getCode()) + "  (change in Options > Controls)", gx + 100, y + 7, A(SUB));
        y += 26;
        row(c, "Notifications", gx, y, gw);
        btn(c, "Test", gx + gw - 58, y + 3, 50, 16, () -> Notify.push("TheFlai1k", "This is a test notification", Config.accent()), mx, my);
        y += 26;
        row(c, "Chat commands", gx, y, gw);
        Gfx.text(c, ".help .gps .friend .wp .coords", gx + 100, y + 7, A(SUB));
        y += 30;
        Gfx.bold(c, "Keybind conflicts", gx, y, A(TXT));
        btn(c, "Rescan", gx + gw - 60, y - 3, 60, 16, () -> confDirty = true, mx, my);
        y += 16;
        if (conflicts.isEmpty()) { Gfx.text(c, "No conflicts found.", gx, y, A(0xFF6EE7A0)); y += 12; }
        for (String s : conflicts) { Gfx.text(c, Gfx.fit(s, gw), gx, y, A(0xFFFFD166)); y += 11; }
        endScroll(c, y - oy + 4, wy + 40);
    }

    // ---------- input ----------
    void applySlider(Sl sl, int mx) { sl.s.setFromFraction((mx - sl.x) / (double) sl.w); }

    @Override
    public boolean mouseClicked(double dx, double dy, int button) {
        int mx = (int) dx, my = (int) dy;
        if (button == 0) {
            for (int i = sls.size() - 1; i >= 0; i--) {
                Sl sl = sls.get(i);
                if (in(mx, my, sl.x - 4, sl.y - 5, sl.w + 8, 14)) { drag = sl; applySlider(sl, mx); return true; }
            }
        }
        for (int i = btns.size() - 1; i >= 0; i--) {
            Btn b = btns.get(i);
            if (!in(mx, my, b.x, b.y, b.w, b.h)) continue;
            Runnable r = button == 1 ? b.r : b.l;
            if (r != null && (button == 0 || button == 1)) { r.run(); return true; }
        }
        return super.mouseClicked(dx, dy, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag != null) { applySlider(drag, (int) mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        drag = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scrollT = MathHelper.clamp(scrollT - (float) v * 24f, 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int sc, int mods) {
        if (binding != null) {
            binding.key = key == GLFW.GLFW_KEY_ESCAPE ? -1 : key;
            binding = null;
            confDirty = true;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !focus.isEmpty()) {
            if (focus.equals("notes")) { if (!Config.notes.isEmpty()) Config.notes = Config.notes.substring(0, Config.notes.length() - 1); }
            else { StringBuilder b = sb(focus); if (b.length() > 0) b.setLength(b.length() - 1); }
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && focus.equals("notes")) {
            if (Config.notes.length() < 4000) Config.notes += "\n";
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && open != null) { open = null; scroll = scrollT = 0; return true; }
        int mk = KeyBindingHelper.getBoundKeyOf(TheFlai1k.menuKey).getCode();
        if (key == mk && mk >= 256 && mk != GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(key, sc, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (binding != null || focus.isEmpty() || chr < 32) return super.charTyped(chr, mods);
        if (focus.equals("notes")) { if (Config.notes.length() < 4000) Config.notes += chr; }
        else { StringBuilder b = sb(focus); if (b.length() < 40) b.append(chr); }
        return true;
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }
}
