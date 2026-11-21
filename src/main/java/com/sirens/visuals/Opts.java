package com.sirens.visuals;

import java.util.List;

public class Opts {
    public record Opt(String label, String f, float min, float max) {
        public boolean isBool() { return max <= min; }
    }
    static Opt b(String l, String f) { return new Opt(l, f, 0, 0); }
    static Opt s(String l, String f, float a, float z) { return new Opt(l, f, a, z); }

    public static final List<Opt> ALL = List.of(
        b("HUD Master", "hudMaster"), b("Armor HUD", "armorHud"), b("Potion HUD", "potionHud"),
        b("Coords HUD", "coordsHud"), b("FPS/Ping HUD", "fpsHud"), b("Keystrokes", "keystrokes"),
        b("CPS Counter", "cpsHud"), b("Sprint Indicator", "sprintHud"), b("Totem Counter", "totemCount"),
        b("Action Bar (Now Playing)", "actionBar"), b("Low Health Vignette", "lowHealth"),
        b("Damage Numbers", "damageNumbers"), b("Hit Marker", "hitMarker"), b("Combo Counter", "comboCounter"),
        b("Fullbright", "fullbright"), b("No Weather", "noWeather"), b("Clear Water", "clearWater"),
        b("Zoom", "zoom"), s("Zoom FOV", "zoomFov", 30, 70), s("Zoom Smoothing", "zoomSmooth", 0.1f, 1f),
        b("Slow Swing", "slowSwing"), s("Swing Slowness", "swingMul", 1, 3),
        b("Hit Ripples", "hitRipples"), s("Ripple Size", "rippleSize", 0.5f, 2f),
        b("Kill Effects", "killEffects"), s("Kill Intensity", "killIntensity", 0.5f, 3f),
        b("Footsteps", "footsteps"), s("Step Spacing", "footInterval", 0.5f, 2f),
        b("Sky Tint", "skyTint"), s("Sky Opacity", "skyAlpha", 0.05f, 0.6f), s("Sky Color Speed", "skySpeed", 0, 5),
        b("Aurora", "aurora"), s("Aurora Strength", "auroraStrength", 0.1f, 1f),
        b("Emotes", "emotes"), b("Music Bridge", "musicBridge"), b("Mute Ads", "muteAds"),
        b("Lyrics in Sky", "lyrics"), b("Lyrics Always Visible", "lyricsAlways"),
        s("Lyrics Fade Pitch", "lyricsFadePitch", 0, 60), s("Lyrics Offset (s)", "lyricsOffset", -3, 3)
    );
}
