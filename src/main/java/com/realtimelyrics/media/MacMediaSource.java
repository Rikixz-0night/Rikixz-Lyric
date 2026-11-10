package com.realtimelyrics.media;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class MacMediaSource extends PollingMediaSource {
   private static final String SCRIPT = "on info(appName)\n\tusing terms from application \"Spotify\"\n\t\ttell application appName\n\t\t\tset t to current track\n\t\t\treturn (player state as string) & tab & (artist of t) & tab & (name of t) & tab & (album of t) & tab & (duration of t) & tab & (player position) & tab & appName\n\t\tend tell\n\tend using terms from\nend info\nif application \"Spotify\" is running then return info(\"Spotify\")\nif application \"Music\" is running then\n\ttell application \"Music\"\n\t\tif player state is not stopped then\n\t\t\tset t to current track\n\t\t\treturn (player state as string) & tab & (artist of t) & tab & (name of t) & tab & (album of t) & tab & ((duration of t) * 1000) & tab & (player position) & tab & \"Music\"\n\t\tend if\n\tend tell\nend if\nreturn \"\"\n";

   @Override
   protected List<String> command() {
      return List.of(
         "osascript",
         "-e",
         "on info(appName)\n\tusing terms from application \"Spotify\"\n\t\ttell application appName\n\t\t\tset t to current track\n\t\t\treturn (player state as string) & tab & (artist of t) & tab & (name of t) & tab & (album of t) & tab & (duration of t) & tab & (player position) & tab & appName\n\t\tend tell\n\tend using terms from\nend info\nif application \"Spotify\" is running then return info(\"Spotify\")\nif application \"Music\" is running then\n\ttell application \"Music\"\n\t\tif player state is not stopped then\n\t\t\tset t to current track\n\t\t\treturn (player state as string) & tab & (artist of t) & tab & (name of t) & tab & (album of t) & tab & ((duration of t) * 1000) & tab & (player position) & tab & \"Music\"\n\t\tend if\n\tend tell\nend if\nreturn \"\"\n"
      );
   }

   @Override
   protected MediaSample parse(String out, long receivedNanos) {
      String[] f = out.split("\t", -1);
      if (f.length < 7) {
         return MediaSample.none();
      }

      boolean playing = f[0].trim().equalsIgnoreCase("playing");
      long duration = (long)Math.max(0.0, parseDouble(f[4]));
      double posSec = parseDouble(f[5]);
      long position = posSec >= 0.0 && duration > 0L ? (long)(posSec * 1000.0) : -1L;
      return new MediaSample(f[6], f[2], f[1], f[3], playing, position, duration, receivedNanos);
   }
}
