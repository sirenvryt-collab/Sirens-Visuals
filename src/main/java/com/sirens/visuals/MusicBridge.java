package com.sirens.visuals;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Receives state from the Tampermonkey script on 127.0.0.1:8765 and fetches synced lyrics from LRCLIB. */
public class MusicBridge {
    public record Line(double t, String s) {}
    public static volatile String id = "", title = "", artist = "", source = "";
    public static volatile double duration, position;
    public static volatile boolean playing, ad;
    public static volatile long recv = 0;
    public static volatile List<Line> lines = List.of();
    public static final Queue<String> cmds = new ConcurrentLinkedQueue<>();
    static volatile String lastKey = "";
    static final HttpClient HC = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    static final Pattern LRC = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)$");

    public static boolean isActive() { return recv != 0 && (System.nanoTime() - recv) < 5_000_000_000L; }
    public static double elapsed() { return position + (playing && !ad ? (System.nanoTime() - recv) / 1e9 : 0); }
    public static double pos() { return elapsed() + Config.lyricsOffset; }

    public static void start() {
        Thread t = new Thread(MusicBridge::loop, "sirens-music");
        t.setDaemon(true);
        t.start();
    }

    static void loop() {
        try (ServerSocket ss = new ServerSocket(8765, 20, InetAddress.getLoopbackAddress())) {
            while (true) {
                Socket s = ss.accept();
                try { s.setSoTimeout(2000); handle(s); } catch (Exception e) {} finally { try { s.close(); } catch (Exception e) {} }
            }
        } catch (Exception e) {
            System.err.println("[Sirens Visuals] music bridge could not start (port 8765 in use?): " + e);
        }
    }

    static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder(); int c;
        while ((c = in.read()) != -1) { if (c == '\n') break; if (c != '\r') sb.append((char) c); }
        return (c == -1 && sb.length() == 0) ? null : sb.toString();
    }

    static void handle(Socket s) throws Exception {
        InputStream in = new BufferedInputStream(s.getInputStream());
        readLine(in);
        int len = 0; String tok = null, l;
        while ((l = readLine(in)) != null && !l.isEmpty()) {
            int c = l.indexOf(':');
            if (c < 0) continue;
            String k = l.substring(0, c).trim().toLowerCase(), v = l.substring(c + 1).trim();
            if (k.equals("content-length")) len = Integer.parseInt(v);
            else if (k.equals("x-ds-token")) tok = v;
        }
        byte[] body = in.readNBytes(len);
        int code = 200; String resp;
        if (!Config.musicBridge || tok == null || !tok.equals(Config.musicToken)) { code = 403; resp = "{}"; }
        else {
            try { update(new String(body, StandardCharsets.UTF_8)); } catch (Exception e) {}
            JsonObject r = new JsonObject();
            r.addProperty("muteAds", Config.muteAds);
            JsonArray ca = new JsonArray();
            String c; while ((c = cmds.poll()) != null) ca.add(c);
            r.add("cmds", ca);
            resp = r.toString();
        }
        byte[] rb = resp.getBytes(StandardCharsets.UTF_8);
        OutputStream out = s.getOutputStream();
        out.write(("HTTP/1.1 " + code + " OK\r\nContent-Type: application/json\r\nContent-Length: " + rb.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(rb);
        out.flush();
    }

    static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    static double num(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0; }
    static boolean bool(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean(); }

    static void update(String body) {
        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        boolean isAd = bool(o, "ad");
        String nt = str(o, "title"), na = str(o, "artist"), ni = str(o, "id");
        source = str(o, "source");
        duration = num(o, "duration"); position = num(o, "position"); playing = bool(o, "playing"); ad = isAd;
        recv = System.nanoTime();
        if (!isAd && !nt.isEmpty()) {
            title = nt; artist = na; id = ni;
            String key = ni + "|" + nt;
            if (!key.equals(lastKey)) {
                lastKey = key; lines = List.of();
                if (Config.lyrics) fetch(key, nt, na, duration);
            }
        }
    }

    static String clean(String s) {
        return s.replaceAll("(?i)\\s*[\\(\\[][^\\)\\]]*(official|video|lyrics|audio|visualizer|hd|4k|mv)[^\\)\\]]*[\\)\\]]", "")
                .replaceAll("(?i)\\s*-\\s*topic$", "").trim();
    }

    static String get(String url) {
        try {
            HttpRequest rq = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "SirensVisuals/1.0").GET().build();
            HttpResponse<String> r = HC.send(rq, HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200 ? r.body() : null;
        } catch (Exception e) { return null; }
    }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static String synced(JsonElement e) {
        if (e == null || !e.isJsonObject()) return null;
        JsonObject o = e.getAsJsonObject();
        return o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull() ? o.get("syncedLyrics").getAsString() : null;
    }

    static void fetch(String key, String t, String a, double dur) {
        CompletableFuture.runAsync(() -> {
            try {
                String ct = clean(t), ca = clean(a), res = null;
                List<String[]> cands = new ArrayList<>();
                cands.add(new String[]{ca, ct});
                if (t.contains(" - ")) { String[] sp = t.split(" - ", 2); cands.add(new String[]{clean(sp[0]), clean(sp[1])}); }
                for (String[] c : cands) {
                    String b = get("https://lrclib.net/api/get?track_name=" + enc(c[1]) + "&artist_name=" + enc(c[0])
                            + (dur > 0 ? "&duration=" + Math.round(dur) : ""));
                    if (b != null) { res = synced(JsonParser.parseString(b)); if (res != null) break; }
                }
                if (res == null) {
                    String b = get("https://lrclib.net/api/search?q=" + enc((ca + " " + ct).trim()));
                    if (b != null) {
                        JsonElement e = JsonParser.parseString(b);
                        if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) { res = synced(x); if (res != null) break; }
                    }
                }
                if (res == null || !key.equals(lastKey)) return;
                List<Line> out = new ArrayList<>();
                for (String ln : res.split("\n")) {
                    Matcher m = LRC.matcher(ln.trim());
                    if (m.matches()) out.add(new Line(Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2)), m.group(3).trim()));
                }
                if (key.equals(lastKey)) lines = out;
            } catch (Exception e) {}
        });
    }
}
