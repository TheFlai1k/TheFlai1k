package com.theflai1k;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
    public HudEditScreen() { super(Text.literal("HUD editor")); }

    @Override public boolean shouldPause() { return false; }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        String t = "HUD editor: drag the panels with the mouse. Esc = back to menu.";
        int w = Gfx.w(t) + 16;
        Gfx.rrect(c, (width - w) / 2, height - 30, w, 18, 5, 0xCC0C0E12);
        Gfx.shadow(c, t, (width - w) / 2 + 8, height - 25, 0xFFFFFFFF);
        String t2 = "Enable more HUD modules in the menu to move them too.";
        Gfx.shadow(c, t2, (width - Gfx.w(t2)) / 2, height - 44, 0xFF9AA3B2);
    }

    @Override public void close() {
        Config.save();
        if (client != null) client.setScreen(new MenuScreen());
    }
}
