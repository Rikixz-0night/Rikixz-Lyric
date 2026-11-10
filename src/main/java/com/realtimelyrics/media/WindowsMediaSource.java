package com.realtimelyrics.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.realtimelyrics.RealTimeLyricsClient;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class WindowsMediaSource implements MediaSource {
   private static final long SIX_HOURS_MS = 21600000L;
   private volatile MediaSample latest;
   private volatile boolean running;
   private volatile Process process;

   @Override
   public MediaSample latest() {
      return this.latest;
   }

   @Override
   public void start() {
      if (!this.running) {
         this.running = true;
         Thread thread = new Thread(this::loop, "RealTimeLyrics-Media");
         thread.setDaemon(true);
         thread.start();
      }
   }

   @Override
   public void stop() {
      this.running = false;
      Process p = this.process;
      if (p != null) {
         p.destroyForcibly();
      }
   }

   private void loop() {
      long backoffMs = 2000L;

      while (this.running) {
         long started = System.currentTimeMillis();

         try {
            this.runHelper();
         } catch (Exception e) {
            RealTimeLyricsClient.LOG.warn("Media watcher stopped: {}", e.toString());
         }

         this.latest = null;
         if (!this.running) {
            break;
         }

         backoffMs = System.currentTimeMillis() - started > 60000L ? 2000L : Math.min(backoffMs * 2L, 30000L);

         try {
            Thread.sleep(backoffMs);
         } catch (InterruptedException e) {
            return;
         }
      }
   }

   private void runHelper() throws Exception {
      Path script = Files.createTempFile("realtimelyrics-media-", ".ps1");
      script.toFile().deleteOnExit();

      try (InputStream in = WindowsMediaSource.class.getResourceAsStream("/assets/realtimelyrics/media_watcher.ps1")) {
         if (in == null) {
            throw new IllegalStateException("media_watcher.ps1 missing from mod jar");
         }

         Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
      }

      ProcessBuilder pb = new ProcessBuilder(
         "powershell.exe",
         "-NoProfile",
         "-NonInteractive",
         "-ExecutionPolicy",
         "Bypass",
         "-File",
         script.toString(),
         "-ParentPid",
         Long.toString(ProcessHandle.current().pid())
      );
      pb.redirectError(Redirect.DISCARD);
      Process p = pb.start();
      this.process = p;

      try {
         String line;
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            while (this.running && (line = reader.readLine()) != null) {
               MediaSample s = parse(line);
               if (s != null) {
                  this.latest = s;
               }
            }
         }
      } finally {
         p.destroyForcibly();
         Files.deleteIfExists(script);
      }
   }

   private static MediaSample parse(String line) {
      line = line.replace("\ufeff", "").trim();
      if (!line.startsWith("{")) {
         return null;
      }

      try {
         JsonObject o = JsonParser.parseString(line).getAsJsonObject();
         long received = System.nanoTime();
         if (o.has("ok") && o.get("ok").getAsBoolean()) {
            boolean playing = "Playing".equalsIgnoreCase(str(o, "status"));
            long duration = num(o, "durationMs");
            long position = -1L;
            if (duration > 0L) {
               position = num(o, "positionMs");
               long updated = num(o, "updatedMs");
               long now = num(o, "nowMs");
               if (playing && updated > 0L && now >= updated && now - updated < 21600000L) {
                  position += now - updated;
               }

               position = Math.max(0L, Math.min(position, duration));
            }

            return new MediaSample(str(o, "app"), str(o, "title"), str(o, "artist"), str(o, "album"), playing, position, duration, received);
         } else {
            return MediaSample.none();
         }
      } catch (Exception e) {
         return null;
      }
   }

   private static String str(JsonObject o, String k) {
      return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
   }

   private static long num(JsonObject o, String k) {
      try {
         return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsLong() : 0L;
      } catch (Exception e) {
         return 0L;
      }
   }
}
