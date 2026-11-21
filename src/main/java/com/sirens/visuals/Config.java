package com.sirens.visuals;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;
import java.nio.file.*;

public class Config {
    public static boolean hudMaster = true, armorHud = true, potionHud = true, coordsHud = true, fpsHud = true,
            keystrokes = true, cpsHud = true, sprintHud = true, totemCount = true, actionBar = true,
            lowHealth = true, damageNumbers = true, hitMarker = true, comboCounter = true;
    public static boolean fullbright = false, noWeather = false, clearWater = true, zoom = true, slowSwing = false,
            hitRipples = true, killEffects = true, footsteps = true, skyTint = true, aurora = true, emotes = true,
            musicBridge = true, lyrics = true, lyricsAlways = false, muteAds = true;
    public static float zoomFov = 30, zoomSmooth = 0.35f, swingMul = 1.8f, rippleSize = 1f, footInterval = 0.9f,
            skyAlpha = 0.35f, skySpeed = 1f, auroraStrength = 0.6f, killIntensity = 1f, lyricsOffset = 0f,
            lyricsFadePitch = 10f;
    public static String musicToken = "36a795c46374b9531a6d9d18256524b0";

    static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("sirensvisuals.json"); }

    public static boolean getB(String f) { try { return Config.class.getField(f).getBoolean(null); } catch (Exception e) { return false; } }
    public static void setB(String f, boolean v) { try { Config.class.getField(f).setBoolean(null, v); } catch (Exception e) {} }
    public static float getF(String f) { try { return Config.class.getField(f).getFloat(null); } catch (Exception e) { return 0; } }
    public static void setF(String f, float v) { try { Config.class.getField(f).setFloat(null, v); } catch (Exception e) {} }

    public static void load() {
        try {
            Path p = file();
            if (!Files.exists(p)) { save(); return; }
            JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            for (Field f : Config.class.getFields()) {
                if (!o.has(f.getName())) continue;
                JsonElement e = o.get(f.getName());
                Class<?> t = f.getType();
                if (t == boolean.class) f.setBoolean(null, e.getAsBoolean());
                else if (t == float.class) f.setFloat(null, e.getAsFloat());
                else if (t == String.class) f.set(null, e.getAsString());
            }
        } catch (Exception ex) {}
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            for (Field f : Config.class.getFields()) {
                Class<?> t = f.getType();
                if (t == boolean.class) o.addProperty(f.getName(), f.getBoolean(null));
                else if (t == float.class) o.addProperty(f.getName(), f.getFloat(null));
                else if (t == String.class) o.addProperty(f.getName(), (String) f.get(null));
            }
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(o));
        } catch (Exception ex) {}
    }
}
