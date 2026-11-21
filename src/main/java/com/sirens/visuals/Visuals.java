package com.sirens.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.*;

public class Visuals {
    static final Random R = new Random();
    static int tickN = 0;
    static LivingEntity target;
    static float hpBefore;
    static int hitTick = -999, combo = 0, lastCombo = -999, marker = 0;
    static final boolean[] ours = new boolean[2];
    static int savedFov = -1;
    static double footDist, lx, lz;
    static boolean leftFoot, attackWas;
    static int emote = -1, emoteT = 0;
    static final ArrayDeque<Long> clicks = new ArrayDeque<>();

    static class Ripple { double x, y, z; int age; }
    static class Pop { String s; int age; float dx; }
    static final List<Ripple> ripples = new ArrayList<>();
    static final List<Pop> pops = new ArrayList<>();

    public static int cps() {
        long n = System.currentTimeMillis();
        while (!clicks.isEmpty() && n - clicks.peekFirst() > 1000) clicks.pollFirst();
        return clicks.size();
    }

    public static void playEmote(int i) { if (Config.emotes) { emote = i; emoteT = 60; } }

    public static void onAttack(Player pl, Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (pl != mc.player || !(e instanceof LivingEntity le)) return;
        target = le; hpBefore = le.getHealth(); hitTick = tickN;
        if (Config.hitRipples) {
            Ripple r = new Ripple(); r.x = e.getX(); r.y = e.getY() + 0.1; r.z = e.getZ();
            ripples.add(r);
        }
    }

    static void eff(LocalPlayer p, Holder<MobEffect> e, boolean on, int idx) {
        MobEffectInstance cur = p.getEffect(e);
        if (on) {
            if (cur == null) { p.addEffect(new MobEffectInstance(e, -1, 0, false, false, false)); ours[idx] = true; }
        } else if (ours[idx]) {
            if (cur != null && cur.getDuration() == -1) p.removeEffect(e);
            ours[idx] = false;
        }
    }

    static void ring(ClientLevel lv, double x, double y, double z, double rad, int n, ParticleOptions o) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            lv.addParticle(o, x + Math.cos(a) * rad, y, z + Math.sin(a) * rad, 0, 0, 0);
        }
    }

    static void head(ClientLevel lv, LocalPlayer p, ParticleOptions o) {
        lv.addParticle(o, p.getX() + (R.nextDouble() - .5) * .6, p.getY() + 2.2, p.getZ() + (R.nextDouble() - .5) * .6, 0, 0.05, 0);
    }

    public static void tick(Minecraft mc) {
        tickN++;
        while (SirensVisuals.MENU.consumeClick()) mc.setScreen(new SettingsScreen(null));
        while (SirensVisuals.EMOTE.consumeClick()) if (Config.emotes) mc.setScreen(new EmoteScreen());
        while (SirensVisuals.FULLBRIGHT.consumeClick()) { Config.fullbright = !Config.fullbright; Config.save(); }
        while (SirensVisuals.HIDEHUD.consumeClick()) mc.options.hideGui = !mc.options.hideGui;
        while (SirensVisuals.PLAY.consumeClick()) MusicBridge.cmds.add(MusicBridge.playing ? "pause" : "play");
        while (SirensVisuals.NEXT.consumeClick()) MusicBridge.cmds.add("next");
        while (SirensVisuals.PREV.consumeClick()) MusicBridge.cmds.add("prev");

        LocalPlayer p = mc.player; ClientLevel lv = mc.level;
        if (p == null || lv == null) return;

        // clicks (CPS)
        boolean down = mc.options.keyAttack.isDown();
        if (down && !attackWas) clicks.add(System.currentTimeMillis());
        attackWas = down;

        // fullbright (client-side night vision) + clear water (client-side conduit vision)
        eff(p, MobEffects.NIGHT_VISION, Config.fullbright, 0);
        eff(p, MobEffects.CONDUIT_POWER, Config.clearWater, 1);

        // no weather
        if (Config.noWeather) { lv.setRainLevel(0f); lv.setThunderLevel(0f); }

        // zoom
        var fov = mc.options.fov();
        boolean z = Config.zoom && SirensVisuals.ZOOM.isDown() && mc.screen == null;
        int cur = fov.get();
        if (z) {
            if (savedFov < 0) savedFov = cur;
            int tgt = (int) Config.zoomFov;
            int nx = Math.round(Mth.lerp(Config.zoomSmooth, cur, tgt));
            if (nx == cur && cur != tgt) nx += tgt < cur ? -1 : 1;
            fov.set(Mth.clamp(nx, 30, 110));
        } else if (savedFov >= 0) {
            int nx = Math.round(Mth.lerp(Config.zoomSmooth, cur, savedFov));
            if (nx == cur && cur != savedFov) nx += savedFov < cur ? -1 : 1;
            fov.set(nx);
            if (nx == savedFov) savedFov = -1;
        }

        // hit tracking: damage numbers, marker, combo, kill effects
        if (marker > 0) marker--;
        if (tickN - lastCombo > 60) combo = 0;
        if (target != null) {
            if (tickN - hitTick <= 10 && target.getHealth() < hpBefore) {
                float d = hpBefore - target.getHealth();
                hpBefore = target.getHealth();
                marker = 6;
                if (Config.comboCounter) { combo++; lastCombo = tickN; }
                if (Config.damageNumbers) {
                    Pop q = new Pop(); q.s = String.format("-%.1f", d); q.dx = R.nextInt(20) - 6; pops.add(q);
                }
            }
            if (target.isDeadOrDying() || target.isRemoved()) {
                if (Config.killEffects) killFx(lv, target);
                target = null;
            } else if (tickN - hitTick > 80) target = null;
        }
        pops.removeIf(q -> ++q.age > 25);

        // ripples
        for (Ripple r : ripples) {
            r.age++;
            double rad = r.age * 0.22 * Config.rippleSize;
            ring(lv, r.x, r.y, r.z, rad, (int) (12 + rad * 14), ParticleTypes.END_ROD);
        }
        ripples.removeIf(r -> r.age > 10);

        // footsteps
        double dx = p.getX() - lx, dz = p.getZ() - lz;
        lx = p.getX(); lz = p.getZ();
        if (Config.footsteps && p.onGround() && !p.isInWater()) {
            footDist += Math.sqrt(dx * dx + dz * dz);
            if (footDist > Config.footInterval) {
                footDist = 0; leftFoot = !leftFoot;
                double yaw = Math.toRadians(p.getYRot()), off = leftFoot ? 0.15 : -0.15;
                double ox = -Math.cos(yaw) * off, oz = -Math.sin(yaw) * off;
                for (int i = 0; i < 3; i++)
                    lv.addParticle(new DustParticleOptions(0x8FD3FF, 0.6f), p.getX() + ox + (R.nextDouble() - .5) * .08,
                            p.getY() + 0.05, p.getZ() + oz + (R.nextDouble() - .5) * .08, 0, 0, 0);
            }
        } else footDist = 0;

        // emotes
        if (emoteT > 0) {
            emoteT--;
            switch (emote) {
                case 0 -> { if (emoteT % 8 == 0) p.swing(InteractionHand.MAIN_HAND); }
                case 1 -> { if (emoteT % 3 == 0) head(lv, p, ParticleTypes.HEART); }
                case 2 -> { if (emoteT % 2 == 0) head(lv, p, ParticleTypes.FIREWORK); }
                case 3 -> { if (emoteT % 4 == 0) head(lv, p, ParticleTypes.ANGRY_VILLAGER); }
                case 4 -> { if (emoteT % 4 == 0) head(lv, p, ParticleTypes.HAPPY_VILLAGER); }
                case 5 -> { if (emoteT % 3 == 0) head(lv, p, ParticleTypes.NOTE); }
                default -> {}
            }
        }
    }

    static void killFx(ClientLevel lv, LivingEntity e) {
        int n = (int) (30 * Config.killIntensity);
        double y = e.getY() + e.getBbHeight() / 2;
        for (int i = 0; i < n; i++)
            lv.addParticle(ParticleTypes.TOTEM_OF_UNDYING, e.getX(), y, e.getZ(),
                    (R.nextDouble() - .5) * .8, R.nextDouble() * .6, (R.nextDouble() - .5) * .8);
        for (int i = 0; i < n / 2; i++)
            lv.addParticle(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), y, e.getZ(),
                    (R.nextDouble() - .5) * .3, R.nextDouble() * .3, (R.nextDouble() - .5) * .3);
    }
}
