package com.realtimelyrics.media;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class PlayerctlMediaSource extends PollingMediaSource {
   @Override
   protected List<String> command() {
      return List.of(
         "playerctl",
         "-a",
         "metadata",
         "--format",
         "{{status}}\t{{artist}}\t{{title}}\t{{album}}\t{{mpris:length}}\t{{position}}\t{{playerName}}"
      );
   }

   @Override
   protected MediaSample parse(String out, long receivedNanos) {
      if (out == null || out.isBlank()) {
         return MediaSample.none();
      }

      String[] lines = out.split("\\r?\\n");
      MediaSample best = MediaSample.none();

      for (String line : lines) {
         String[] f = line.split("\t", -1);
         if (f.length < 7) {
            continue;
         }

         String status = f[0].trim();
         String artist = f[1].trim();
         String title = f[2].trim();
         String album = f[3].trim();
         String player = f[6].trim();

         if (title.isEmpty()) {
            continue;
         }

         // Ignore chat apps or voice notes that aren't music
         boolean isTelegram = player.toLowerCase().contains("telegram");
         if (isTelegram && title.startsWith("today at")) {
            continue;
         }

         boolean playing = "Playing".equalsIgnoreCase(status);
         long lengthUs = parseLong(f[4]);
         long positionUs = parseLong(f[5]);
         long duration = lengthUs > 0L ? lengthUs / 1000L : 0L;
         long position = positionUs >= 0L && duration > 0L ? positionUs / 1000L : -1L;

         MediaSample sample = new MediaSample(player, title, artist, album, playing, position, duration, receivedNanos);

         if (playing) {
            return sample; // Immediately select the currently playing media player!
         } else if (best == MediaSample.none()) {
            best = sample;
         }
      }

      return best;
   }
}
