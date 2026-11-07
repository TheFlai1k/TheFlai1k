package com.theflai1k;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TheFlai1k implements ClientModInitializer {
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.theflai1k.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.theflai1k"));
        Modules.init();
        Config.load();
        ClientTickEvents.END_CLIENT_TICK.register(Tick::run);
        HudRenderCallback.EVENT.register((ctx, tc) -> Hud.render(ctx));
        ClientSendMessageEvents.ALLOW_CHAT.register(Commands::handle);
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> { if (!overlay) Tools.incoming(msg.getString()); });
        ClientReceiveMessageEvents.CHAT.register((msg, signed, sender, params, time) -> Tools.incoming(msg.getString()));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> { Tools.shutdown(); Config.save(); });
    }
}
