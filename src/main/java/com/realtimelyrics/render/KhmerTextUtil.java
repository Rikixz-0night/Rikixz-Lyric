package com.realtimelyrics.render;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KhmerTextUtil {
   private static final Logger LOGGER = LoggerFactory.getLogger("RikixzLyric");
   private static Font font;
   private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
   private static final Pattern KHMER_RUN = Pattern.compile("[\u1780-\u17FF\u19E0-\u19FF]+");
   private static final Pattern KHMER_CLUSTER = Pattern.compile("[\u1780-\u17B3\u17E0-\u17E9](?:\u17D2[\u1780-\u17A2]|[\u17B6-\u17D3\u17DD])*");

   static {
      try {
         InputStream in = KhmerTextUtil.class.getResourceAsStream("/assets/rikixz_lyric/font/NotoSansKhmer-Bold.ttf");
         if (in == null) {
            in = KhmerTextUtil.class.getResourceAsStream("/assets/realtimelyrics/font/NotoSansKhmer-Bold.ttf");
         }
         if (in == null) {
            in = KhmerTextUtil.class.getResourceAsStream("/assets/minecraft/font/NotoSansKhmer-Bold.ttf");
         }
         final InputStream fontStream = in;
         if (fontStream != null) {
            try (fontStream) {
               font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
               LOGGER.info("Loaded Khmer shaping font: {}", font.getFontName());
            }
         } else {
            LOGGER.warn("Could not find NotoSansKhmer-Bold.ttf in mod resources!");
         }
      } catch (Exception e) {
         LOGGER.error("Failed to load Khmer font for shaping", e);
      }
   }

   private KhmerTextUtil() {}

   public static boolean containsKhmer(String text) {
      if (text == null) return false;
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if ((c >= 0x1780 && c <= 0x17FF) || (c >= 0x19E0 && c <= 0x19FF)) {
            return true;
         }
      }
      return false;
   }

   public static synchronized String shape(String text) {
      if (text == null || text.isEmpty() || !containsKhmer(text)) {
         return text;
      }
      try {
         // Try ICU4J normalization first
         String normalized = com.ibm.icu.text.Normalizer2.getNFCInstance().normalize(text);
         return normalized;
      } catch (Throwable t) {
         try {
            java.text.Normalizer.Form form = java.text.Normalizer.Form.NFC;
            return java.text.Normalizer.isNormalized(text, form) ? text : java.text.Normalizer.normalize(text, form);
         } catch (Throwable t2) {
            return text;
         }
      }
   }

   public static List<String> tokenize(String text) {
      if (text == null || text.isBlank()) {
         return Collections.emptyList();
      }
      String[] parts = text.trim().split("\\s+");
      List<String> tokens = new ArrayList<>(parts.length);
      for (String part : parts) {
         if (part.isBlank()) continue;
         if (containsKhmer(part)) {
            Matcher cm = KHMER_CLUSTER.matcher(part);
            List<String> clusters = new ArrayList<>();
            int lastEnd = 0;
            while (cm.find()) {
               if (cm.start() > lastEnd) {
                  clusters.add(part.substring(lastEnd, cm.start()));
               }
               clusters.add(cm.group());
               lastEnd = cm.end();
            }
            if (lastEnd < part.length()) {
               clusters.add(part.substring(lastEnd));
            }

            if (clusters.size() > 3) {
               int i = 0;
               while (i < clusters.size()) {
                  int rem = clusters.size() - i;
                  if (rem == 3) {
                     tokens.add(clusters.get(i) + clusters.get(i + 1) + clusters.get(i + 2));
                     i += 3;
                  } else {
                     int take = Math.min(2, rem);
                     StringBuilder chunk = new StringBuilder();
                     for (int k = 0; k < take; k++) {
                        chunk.append(clusters.get(i + k));
                     }
                     tokens.add(chunk.toString());
                     i += take;
                  }
               }
            } else {
               tokens.add(part);
            }
         } else {
            tokens.add(part);
         }
      }
      return tokens;
   }
}
