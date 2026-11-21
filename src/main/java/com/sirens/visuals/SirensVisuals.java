package com.sirens.visuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class SirensVisuals implements ClientModInitializer {
    public static KeyMapping MENU, ZOOM, EMOTE, FULLBRIGHT, HIDEHUD, PLAY, NEXT, PREV;

    static KeyMapping reg(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYSYM, key, KeyMapping.Category.MISC));
    }

    @Override
    public void onInitializeClient() {
        Config.load();
        MENU = reg("key.sirensvisuals.menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        ZOOM = reg("key.sirensvisuals.zoom", GLFW.GLFW_KEY_C);
        EMOTE = reg("key.sirensvisuals.emote", GLFW.GLFW_KEY_V);
        FULLBRIGHT = reg("key.sirensvisuals.fullbright", GLFW.GLFW_KEY_B);
        HIDEHUD = reg("key.sirensvisuals.hidehud", GLFW.GLFW_KEY_F8);
        PLAY = reg("key.sirensvisuals.play", GLFW.GLFW_KEY_KP_5);
        NEXT = reg("key.sirensvisuals.next", GLFW.GLFW_KEY_KP_6);
        PREV = reg("key.sirensvisuals.prev", GLFW.GLFW_KEY_KP_4);
        MusicBridge.start();
        ClientTickEvents.END_CLIENT_TICK.register(Visuals::tick);
        HudRenderCallback.EVENT.register((g, t) -> Hud.render(g));
        AttackEntityCallback.EVENT.register((pl, w, h, e, hit) -> {
            Visuals.onAttack(pl, e);
            return InteractionResult.PASS;
        });
    }
}
