package com.realtimelyrics.lyrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record Lyrics(List<Lyrics.Line> lines, boolean synced, boolean instrumental) {
   private static final Pattern TIME_TAG = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2}(?:[.:]\\d{1,3})?)]");
   private static final Pattern WORD_TAG = Pattern.compile("<\\d{1,3}:\\d{1,2}(?:[.:]\\d{1,3})?>");

   public static Lyrics instrumentalSong() {
      return new Lyrics(List.of(), true, true);
   }

   public int indexAt(long ms) {
      int lo = 0;
      int hi = this.lines.size() - 1;
      int found = -1;

      while (lo <= hi) {
         int mid = lo + hi >>> 1;
         if (this.lines.get(mid).timeMs() <= ms) {
            found = mid;
            lo = mid + 1;
         } else {
            hi = mid - 1;
         }
      }

      return found;
   }

   public static Lyrics parseLrc(String lrc) {
      List<Lyrics.Line> out = new ArrayList<>();

      for (String raw : lrc.split("\\r?\\n")) {
         Matcher m = TIME_TAG.matcher(raw);
         List<Long> times = new ArrayList<>();

         int end;
         for (end = 0; m.find() && m.start() == end; end = m.end()) {
            long min = Long.parseLong(m.group(1));
            double sec = Double.parseDouble(m.group(2).replace(':', '.'));
            times.add(min * 60000L + Math.round(sec * 1000.0));
         }

         if (!times.isEmpty()) {
            String text = WORD_TAG.matcher(raw.substring(end)).replaceAll("").trim();

            for (long t : times) {
               out.add(new Lyrics.Line(t, text));
            }
         }
      }

      out.sort(Comparator.comparingLong(Lyrics.Line::timeMs));
      return new Lyrics(List.copyOf(out), true, false);
   }

   public static Lyrics fromPlain(String plain, long durationMs) {
      List<String> texts = new ArrayList<>();

      for (String s : plain.split("\\r?\\n")) {
         if (!s.isBlank()) {
            texts.add(s.trim());
         }
      }

      List<Lyrics.Line> out = new ArrayList<>();
      if (texts.isEmpty()) {
         return new Lyrics(List.of(), false, false);
      }

      long start = durationMs > 0L ? (long)(durationMs * 0.06) : 8000L;
      long step = durationMs > 0L ? Math.max(1500L, (long)(durationMs * 0.88) / texts.size()) : 3500L;

      for (int i = 0; i < texts.size(); i++) {
         out.add(new Lyrics.Line(start + i * step, texts.get(i)));
      }

      return new Lyrics(List.copyOf(out), false, false);
   }

   @Environment(EnvType.CLIENT)
   public record Line(long timeMs, String text) {
      public boolean isBlank() {
         return this.text.isBlank();
      }
   }
}
