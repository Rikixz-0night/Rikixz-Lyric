package com.realtimelyrics.media;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record MediaSample(String app, String title, String artist, String album, boolean playing, long positionMs, long durationMs, long receivedNanos) {
   public static MediaSample none() {
      return new MediaSample("", "", "", "", false, -1L, 0L, System.nanoTime());
   }

   public boolean hasTrack() {
      return this.title != null && !this.title.isBlank();
   }

   public boolean positionKnown() {
      return this.positionMs >= 0L;
   }

   public long positionAt(long nowNanos) {
      if (this.positionMs < 0L) {
         return -1L;
      }

      long pos = this.positionMs + (this.playing ? (nowNanos - this.receivedNanos) / 1000000L : 0L);
      return this.durationMs > 0L ? Math.min(pos, this.durationMs) : pos;
   }
}
