package com.realtimelyrics.media;

import com.realtimelyrics.RealTimeLyricsClient;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
abstract class PollingMediaSource implements MediaSource {
   private volatile MediaSample latest;
   private volatile boolean running;
   private boolean warned;

   protected abstract List<String> command();

   protected abstract MediaSample parse(String var1, long var2);

   @Override
   public MediaSample latest() {
      return this.latest;
   }

   @Override
   public void start() {
      if (!this.running) {
         this.running = true;
         Thread t = new Thread(this::loop, "RealTimeLyrics-Media");
         t.setDaemon(true);
         t.start();
      }
   }

   @Override
   public void stop() {
      this.running = false;
   }

   private void loop() {
      while (this.running) {
         try {
            Process p = new ProcessBuilder(this.command()).redirectError(Redirect.DISCARD).start();
            if (p.waitFor(3L, TimeUnit.SECONDS)) {
               String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
               this.latest = p.exitValue() == 0 && !out.isEmpty() ? this.parse(out, System.nanoTime()) : MediaSample.none();
            } else {
               p.destroyForcibly();
            }
         } catch (IOException e) {
            if (!this.warned) {
               this.warned = true;
               RealTimeLyricsClient.LOG.warn("Can't read now playing ({}): {}", this.command().get(0), e.toString());
            }

            this.latest = null;
         } catch (Exception e) {
            this.latest = null;
         }

         try {
            Thread.sleep(500L);
         } catch (InterruptedException e) {
            return;
         }
      }
   }

   protected static long parseLong(String s) {
      try {
         return Long.parseLong(s.trim());
      } catch (Exception e) {
         return -1L;
      }
   }

   protected static double parseDouble(String s) {
      try {
         return Double.parseDouble(s.trim().replace(',', '.'));
      } catch (Exception e) {
         return -1.0;
      }
   }
}
