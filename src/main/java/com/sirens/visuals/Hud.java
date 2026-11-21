package com.sirens.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

public class Hud {
    static int frames, fps;
    static long fpsT = System.currentTimeMillis();

    static int argb(int a, int rgb) { return (Mth.clamp(a, 0, 255) << 24) | (rgb & 0xFFFFFF); }
    static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui) return;
        frames++;
        long now = System.currentTimeMillis();
        if (now - fpsT >= 1000) { fps = frames; frames = 0; fpsT = now; }
        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();

        if (Config.skyTint) sky(g, mc, W, H);
        if (Config.lyrics) lyrics(g, mc, W, H);
        if (Config.lowHealth) vignette(g, p, W, H);
        if (!Config.hudMaster) return;

        // left info lines
        List<String> L = new ArrayList<>();
        if (Config.coordsHud)
            L.add(String.format("XYZ %.1f %.1f %.1f  %s", p.getX(), p.getY(), p.getZ(), cap(p.getDirection().getName())));
        if (Config.fpsHud) {
            String s = fps + " FPS";
            var cn = mc.getConnection();
            if (cn != null) { var pi = cn.getPlayerInfo(p.getUUID()); if (pi != null) s += "  " + pi.getLatency() + " ms"; }
            L.add(s);
        }
        if (Config.cpsHud) L.add("CPS " + Visuals.cps());
        if (Config.sprintHud && p.isSprinting()) L.add("Sprinting");
        if (Config.totemCount) {
            int n = 0; var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack s = inv.getItem(i); if (s.is(Items.TOTEM_OF_UNDYING)) n += s.getCount(); }
            if (n > 0) L.add("Totems " + n);
        }
        for (int i = 0; i < L.size(); i++) g.drawString(mc.font, L.get(i), 4, 4 + i * 10, 0xFFFFFFFF, true);

        // potions (right)
        int py = 4;
        for (MobEffectInstance e : p.getActiveEffects()) {
            if (!Config.potionHud) break;
            if (!e.isVisible()) continue;
            int d = e.getDuration() / 20;
            String s = e.getEffect().value().getDisplayName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "")
                    + "  " + (e.isInfiniteDuration() ? "inf" : String.format("%d:%02d", d / 60, d % 60));
            g.drawString(mc.font, s, W - mc.font.width(s) - 4, py, 0xFFFFFFFF, true);
            py += 10;
        }

        // armor (left middle)
        if (Config.armorHud) {
            EquipmentSlot[] sl = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) {
                ItemStack st = p.getItemBySlot(sl[i]);
                if (st.isEmpty()) continue;
                int y = H / 2 - 40 + i * 20;
                g.renderItem(st, 4, y);
                g.renderItemDecorations(mc.font, st, 4, y);
                if (st.isDamageableItem()) {
                    int pct = (st.getMaxDamage() - st.getDamageValue()) * 100 / st.getMaxDamage();
                    g.drawString(mc.font, pct + "%", 24, y + 4, 0xFFFFFFFF, true);
                }
            }
        }

        // keystrokes (bottom left)
        if (Config.keystrokes) {
            key(g, mc, 22, H - 60, "W", mc.options.keyUp.isDown());
            key(g, mc, 4, H - 42, "A", mc.options.keyLeft.isDown());
            key(g, mc, 22, H - 42, "S", mc.options.keyDown.isDown());
            key(g, mc, 40, H - 42, "D", mc.options.keyRight.isDown());
        }

        // action bar: now playing
        if (Config.actionBar && MusicBridge.isActive() && !MusicBridge.title.isEmpty()) {
            int y = H - 76;
            if (MusicBridge.ad) g.drawCenteredString(mc.font, "Ad playing (muted)", W / 2, y, 0xFFAAAAAA);
            else {
                String s = "\u266A " + MusicBridge.title + (MusicBridge.artist.isEmpty() ? "" : " - " + MusicBridge.artist);
                g.drawCenteredString(mc.font, s, W / 2, y, 0xFFFFFFFF);
                double d = MusicBridge.duration;
                if (d > 0) {
                    int bw = 140, bx = W / 2 - bw / 2;
                    g.fill(bx, y + 11, bx + bw, y + 13, 0x66FFFFFF);
                    g.fill(bx, y + 11, bx + (int) (bw * Mth.clamp(MusicBridge.elapsed() / d, 0, 1)), y + 13, 0xFFFFFFFF);
                }
            }
        }

        // damage numbers, marker, combo
        for (Visuals.Pop q : Visuals.pops)
            g.drawString(mc.font, q.s, (int) (W / 2 + 14 + q.dx), (int) (H / 2 - 6 - q.age * 0.9f),
                    argb((int) (255 * (1 - q.age / 25f)), 0xFF5555), true);
        if (Config.hitMarker && Visuals.marker > 0)
            g.drawCenteredString(mc.font, "x", W / 2, H / 2 - 4, argb(255 * Visuals.marker / 6, 0xFFFFFF));
        if (Config.comboCounter && Visuals.combo > 1)
            g.drawCenteredString(mc.font, "Combo x" + Visuals.combo, W / 2, H / 2 + 16, 0xFFFFAA00);
    }

    static void key(GuiGraphics g, Minecraft mc, int x, int y, String s, boolean on) {
        g.fill(x, y, x + 16, y + 16, on ? 0xAAFFFFFF : 0x66000000);
        g.drawCenteredString(mc.font, s, x + 8, y + 4, on ? 0xFF000000 : 0xFFFFFFFF);
    }

    static void vignette(GuiGraphics g, LocalPlayer p, int W, int H) {
        float hp = p.getHealth();
        if (hp > 6) return;
        float k = (6 - hp) / 6f, pulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 200.0);
        int a = (int) (140 * k * pulse), h = H / 5;
        g.fillGradient(0, 0, W, h, argb(a, 0xFF0000), argb(0, 0xFF0000));
        g.fillGradient(0, H - h, W, H, argb(0, 0xFF0000), argb(a, 0xFF0000));
    }

    static void sky(GuiGraphics g, Minecraft mc, int W, int H) {
        float up = Mth.clamp((-mc.player.getXRot() - 5f) / 40f, 0, 1);
        if (up <= 0) return;
        float t = (System.currentTimeMillis() % 600000) / 1000f * Config.skySpeed;
        int rgb = Mth.hsvToRgb((t * 0.02f) % 1f, 0.55f, 1f);
        g.fillGradient(0, 0, W, (int) (H * 0.7f), argb((int) (Config.skyAlpha * up * 255), rgb), argb(0, rgb));
        if (!Config.aurora) return;
        int step = 6;
        for (int x = 0; x < W; x += step) {
            float w1 = (float) Math.sin(x * 0.02 + t * 0.8) * 0.5f + 0.5f, w2 = (float) Math.sin(x * 0.05 - t * 1.3) * 0.5f + 0.5f;
            int h = (int) (H * (0.15f + 0.3f * w1 * up)), top = (int) (H * 0.05f + w2 * H * 0.1f);
            int al = (int) (Config.auroraStrength * up * (70 + 110 * w2)), c = Mth.hsvToRgb(0.33f + 0.2f * w1, 0.7f, 1f);
            g.fillGradient(x, top, x + step, top + h / 2, argb(0, c), argb(al, c));
            g.fillGradient(x, top + h / 2, x + step, top + h, argb(al, c), argb(0, c));
        }
    }

    static void lyrics(GuiGraphics g, Minecraft mc, int W, int H) {
        if (!MusicBridge.isActive() || MusicBridge.ad) return;
        List<MusicBridge.Line> ls = MusicBridge.lines;
        if (ls.isEmpty()) return;
        float vis = Config.lyricsAlways ? 1f : Mth.clamp((-mc.player.getXRot() - Config.lyricsFadePitch) / 25f, 0, 1);
        if (vis <= 0.02f) return;
        double pos = MusicBridge.pos();
        int idx = -1;
        for (int i = 0; i < ls.size(); i++) { if (ls.get(i).t() <= pos) idx = i; else break; }
        int y = (int) (H * 0.14f), a = (int) (255 * vis);
        line(g, mc, W, y - 12, idx - 1, ls, (int) (a * 0.45f), 0xBBBBBB);
        line(g, mc, W, y, idx, ls, a, 0xFFFFFF);
        line(g, mc, W, y + 12, idx + 1, ls, (int) (a * 0.45f), 0xBBBBBB);
    }

    static void line(GuiGraphics g, Minecraft mc, int W, int y, int i, List<MusicBridge.Line> ls, int a, int rgb) {
        if (i < 0 || i >= ls.size()) return;
        String s = ls.get(i).s();
        if (!s.isEmpty()) g.drawCenteredString(mc.font, s, W / 2, y, argb(a, rgb));
    }
}
