package com.realtimelyrics.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.realtimelyrics.RealTimeLyricsClient;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class LrcLibClient {
   private static final String BASE = "https://lrclib.net/api/";
   private static final String USER_AGENT = "RealTimeLyrics/1.0 (Minecraft Fabric mod)";
   private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).followRedirects(Redirect.NORMAL).build();
   private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "RealTimeLyrics-Fetch");
      t.setDaemon(true);
      return t;
   });
   private final Map<String, Optional<Lyrics>> cache = new LinkedHashMap<String, Optional<Lyrics>>(64, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, Optional<Lyrics>> eldest) {
         return this.size() > 64;
      }
   };

   public CompletableFuture<Optional<Lyrics>> find(String artist, String title, String album, long durationMs) {
      String key = (artist + "\n" + title).toLowerCase();
      return CompletableFuture.supplyAsync(() -> {
         synchronized (this.cache) {
            Optional<Lyrics> hit = this.cache.get(key);
            if (hit != null) {
               return hit;
            }
         }

         Optional<Lyrics> result;
         try {
            result = Optional.ofNullable(this.lookup(artist, title, album, durationMs));
         } catch (Exception e) {
            RealTimeLyricsClient.LOG.warn("Lyrics lookup failed for {} - {}: {}", new Object[]{artist, title, e.toString()});
            return Optional.empty();
         }

         synchronized (this.cache) {
            this.cache.put(key, result);
            return result;
         }
      }, this.executor);
   }

   private Lyrics lookup(String artist, String title, String album, long durationMs) throws Exception {
      List<String[]> candidates = TitleCleaner.candidates(artist, title);
      JsonObject plainFallback = null;

      for (String[] c : candidates) {
         if (!c[0].isEmpty()) {
            StringBuilder q = new StringBuilder("get?artist_name=").append(enc(c[0])).append("&track_name=").append(enc(c[1]));
            if (album != null && !album.isBlank()) {
               q.append("&album_name=").append(enc(album));
            }

            if (durationMs > 0L) {
               q.append("&duration=").append(Math.round(durationMs / 1000.0));
            }

            JsonElement r = this.request(q.toString());
            if (r == null && (album != null && !album.isBlank() || durationMs > 0L)) {
               r = this.request("get?artist_name=" + enc(c[0]) + "&track_name=" + enc(c[1]));
            }

            if (r != null && r.isJsonObject()) {
               JsonObject o = r.getAsJsonObject();
               if (bool(o, "instrumental")) {
                  return Lyrics.instrumentalSong();
               }

               Lyrics synced = synced(o);
               if (synced != null) {
                  return synced;
               }

               if (plainFallback == null && has(o, "plainLyrics")) {
                  plainFallback = o;
               }
            }
         }
      }

      for (String[] c : candidates) {
         String q = c[0].isEmpty() ? "search?q=" + enc(c[1]) : "search?track_name=" + enc(c[1]) + "&artist_name=" + enc(c[0]);
         JsonObject best = pickBest(this.request(q), durationMs);
         if (best == null && !c[0].isEmpty()) {
            best = pickBest(this.request("search?q=" + enc(c[0] + " " + c[1])), durationMs);
         }

         if (best != null) {
            Lyrics synced = synced(best);
            if (synced != null) {
               return synced;
            }

            if (plainFallback == null && has(best, "plainLyrics")) {
               plainFallback = best;
            }
         }
      }

      if (plainFallback != null) {
         if (bool(plainFallback, "instrumental")) {
            return Lyrics.instrumentalSong();
         }

         if (has(plainFallback, "plainLyrics")) {
            return Lyrics.fromPlain(plainFallback.get("plainLyrics").getAsString(), durationMs);
         }
      }

      return null;
   }

   private static JsonObject pickBest(JsonElement results, long durationMs) {
      if (results != null && results.isJsonArray()) {
         JsonArray arr = results.getAsJsonArray();
         JsonObject best = null;
         double bestScore = Double.MAX_VALUE;

         for (JsonElement e : arr) {
            if (e.isJsonObject()) {
               JsonObject o = e.getAsJsonObject();
               boolean synced = synced(o) != null;
               if (synced || has(o, "plainLyrics") || bool(o, "instrumental")) {
                  double score = synced ? 0.0 : 1000.0;
                  if (durationMs > 0L && o.has("duration") && !o.get("duration").isJsonNull()) {
                     double diff = Math.abs(o.get("duration").getAsDouble() - durationMs / 1000.0);
                     if (diff > 15.0) {
                        continue;
                     }

                     score += diff;
                  }

                  if (score < bestScore) {
                     bestScore = score;
                     best = o;
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private static Lyrics synced(JsonObject o) {
      if (!has(o, "syncedLyrics")) {
         return null;
      }

      Lyrics l = Lyrics.parseLrc(o.get("syncedLyrics").getAsString());
      return l.lines().stream().filter(x -> !x.isBlank()).count() >= 4L ? l : null;
   }

   private JsonElement request(String pathAndQuery) throws Exception {
      HttpRequest req = HttpRequest.newBuilder(URI.create("https://lrclib.net/api/" + pathAndQuery))
         .timeout(Duration.ofSeconds(10L))
         .header("User-Agent", "RealTimeLyrics/1.0 (Minecraft Fabric mod)")
         .GET()
         .build();
      HttpResponse<String> res = this.http.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (res.statusCode() == 404) {
         return null;
      } else if (res.statusCode() / 100 != 2) {
         throw new IllegalStateException("HTTP " + res.statusCode());
      } else {
         return JsonParser.parseString(res.body());
      }
   }

   private static boolean has(JsonObject o, String k) {
      return o.has(k) && !o.get(k).isJsonNull() && !o.get(k).getAsString().isBlank();
   }

   private static boolean bool(JsonObject o, String k) {
      return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean();
   }

   private static String enc(String s) {
      return URLEncoder.encode(s, StandardCharsets.UTF_8);
   }
}
