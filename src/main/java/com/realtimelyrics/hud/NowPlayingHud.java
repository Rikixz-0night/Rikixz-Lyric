package com.realtimelyrics.hud;

import com.realtimelyrics.LyricsConfig;
import com.realtimelyrics.LyricsController;
import com.realtimelyrics.lyrics.Lyrics;
import com.realtimelyrics.media.MediaSample;
import com.realtimelyrics.render.Anim;
import com.realtimelyrics.render.KhmerTextUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class NowPlayingHud implements HudElement {
   public static final int WIDTH = 256;
   public static final int HEIGHT = 54;
   private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("rikixz_lyric", "textures/gui/logo.png");

   private final LyricsConfig config;
   private final LyricsController controller;

   public NowPlayingHud(LyricsConfig config, LyricsController controller) {
      this.config = config;
      this.controller = controller;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
      if (!this.config.enabled || !this.config.hudEnabled) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.gui == null || mc.gui.hud == null || mc.gui.hud.isHidden()) {
         return;
      }

      MediaSample media = this.controller.currentMedia();
      boolean hasTrack = media != null && media.hasTrack();
      if (!hasTrack && !this.config.hudAlwaysShow) {
         return;
      }

      float scale = (float)this.config.hudScale;
      int w = (int)(WIDTH * scale);
      int h = (int)(HEIGHT * scale);
      int x = (int)(g.guiWidth() * this.config.hudPercentX - w / 2.0f);
      int y = (int)(g.guiHeight() * this.config.hudPercentY - h / 2.0f);

      x = Math.max(2, Math.min(g.guiWidth() - w - 2, x));
      y = Math.max(2, Math.min(g.guiHeight() - h - 2, y));

      this.renderHudCard(g, x, y, scale, false, false);
   }

   public void renderHudCard(GuiGraphicsExtractor g, int x, int y, float scale, boolean isDragging, boolean isPreview) {
      Minecraft mc = Minecraft.getInstance();
      Font font = mc.font;
      MediaSample media = this.controller.currentMedia();
      boolean hasTrack = media != null && media.hasTrack();

      String rawTitle = hasTrack ? this.controller.displayTitle() : (isPreview ? "Girl In My Dream 2" : "No Media Playing");
      String rawArtist = hasTrack ? this.controller.displayArtist() : (isPreview ? "1NE" : "Play music in Spotify / Browser");
      String rawAlbum = hasTrack && media.album() != null ? media.album() : (isPreview ? "Ambition" : "");
      long durationMs = hasTrack ? media.durationMs() : (isPreview ? 223000L : 0L);
      long positionMs = hasTrack ? this.controller.songPositionMs() : (isPreview ? 98000L : 0L);
      boolean isPlaying = hasTrack ? this.controller.isPlaying() : isPreview;
      String app = hasTrack && media.app() != null && !media.app().isBlank() ? media.app().toUpperCase() : (isPreview ? "FIREFOX" : "");

      String title = KhmerTextUtil.shape(rawTitle);
      String artist = KhmerTextUtil.shape(rawArtist);

      // Sub song: artist + album / subtitle
      String subSongText = artist;
      if (!rawAlbum.isBlank() && !rawAlbum.equalsIgnoreCase(rawTitle)) {
         subSongText = artist + " · " + rawAlbum;
      }
      String subSong = KhmerTextUtil.shape(subSongText);

      // Live sub-lyric (karaoke subtitle) line
      String liveLyric = null;
      Lyrics lyrics = this.controller.lyrics();
      if (lyrics != null && !lyrics.lines().isEmpty()) {
         int idx = lyrics.indexAt(positionMs);
         if (idx >= 0 && idx < lyrics.lines().size()) {
            String lText = lyrics.lines().get(idx).text();
            if (lText != null && !lText.isBlank()) {
               liveLyric = KhmerTextUtil.shape(lText.trim());
            }
         }
      } else if (isPreview) {
         liveLyric = KhmerTextUtil.shape("ស្រឡាញ់អូនដល់ចាស់ ♪");
      }

      g.pose().pushMatrix();
      g.pose().translate((float)x, (float)y);
      g.pose().scale(scale, scale);

      // Outer drop shadow glow
      g.fill(-2, -2, WIDTH + 2, HEIGHT + 2, isDragging ? 0x6000F2FE : 0x25000000);

      // Rounded dark glass card body
      g.fill(2, 0, WIDTH - 2, HEIGHT, 0xEE0D0F18);
      g.fill(0, 2, WIDTH, HEIGHT - 2, 0xEE0D0F18);
      g.fill(1, 1, WIDTH - 1, HEIGHT - 1, 0xEE0D0F18);

      // Card border
      int borderColor = isDragging ? 0xFFFFD700 : (hasTrack && isPlaying ? 0x5500F2FE : 0x22FFFFFF);
      g.outline(0, 0, WIDTH, HEIGHT, borderColor);

      // Animated neon gradient stripe on top
      long millis = System.currentTimeMillis();
      float hue1 = (millis % 7000L) / 7000.0f;
      float hue2 = (hue1 + 0.32f) % 1.0f;
      int c1 = 0xFF000000 | Anim.hsv(hue1, 0.85f, 1.0f);
      int c2 = 0xFF000000 | Anim.hsv(hue2, 0.85f, 1.0f);
      g.fillGradient(2, 0, WIDTH - 2, 2, c1, c2);

      // Left Logo / Album Artwork Box (size: 42x42)
      int logoX = 6;
      int logoY = 6;
      int logoSize = 42;

      // Dark artwork panel backing & border
      g.fill(logoX, logoY, logoX + logoSize, logoY + logoSize, 0x66000000);
      g.outline(logoX, logoY, logoSize, logoSize, 0x444FACFE);

      // Render the mod's official logo image!
      try {
         g.blit(LOGO_TEXTURE, logoX, logoY, logoX + logoSize, logoY + logoSize, 0.0F, 1.0F, 0.0F, 1.0F);
      } catch (Throwable ignored) {
         // Fallback if texture is loading
         g.fill(logoX + 1, logoY + 1, logoX + logoSize - 1, logoY + logoSize - 1, 0xFF1E293B);
      }

      // Mini Equalizer badge in bottom-right corner of the logo
      if (isPlaying) {
         int eqW = 16;
         int eqH = 10;
         int eqX = logoX + logoSize - eqW;
         int eqY = logoY + logoSize - eqH;
         g.fill(eqX, eqY, eqX + eqW, eqY + eqH, 0xB0000000);

         float t = (millis % 60000L) / 1000.0f;
         for (int b = 0; b < 3; b++) {
            float wave = (float)(Math.sin(t * 8.0f + b * 1.5f) * 0.5f + 0.5f);
            int barH = Math.max(2, (int)(8 * wave));
            int barX = eqX + 2 + b * 5;
            int barY = eqY + eqH - barH - 1;
            g.fill(barX, barY, barX + 3, eqY + eqH - 1, 0xFF00E676);
         }
      }

      // App Source Badge Pill (e.g. SPOTIFY, FIREFOX)
      if (!app.isEmpty()) {
         int tagW = font.width(app) + 8;
         int tagX = WIDTH - tagW - 6;
         g.fill(tagX, 6, tagX + tagW, 16, 0x401DB954);
         g.outline(tagX, 6, tagW, 10, 0x601DB954);
         g.text(font, Component.literal(app).withStyle(ChatFormatting.GREEN), tagX + 4, 7, 0xFF1DB954);
      }

      int textX = 54;
      int maxTitleW = app.isEmpty() ? (WIDTH - textX - 8) : (WIDTH - textX - font.width(app) - 18);

      // Line 1: Main Title
      String titleClipped = clipText(font, title, maxTitleW);
      g.text(font, Component.literal(titleClipped).withStyle(ChatFormatting.BOLD), textX, 6, 0xFFFFFFFF);

      // Line 2: Sub song (Artist • Album / Subtitle)
      String subSongClipped = clipText(font, subSong, WIDTH - textX - 8);
      g.text(font, Component.literal(subSongClipped), textX, 17, 0xFFB4BACD);

      // Line 3: Live Sub-Lyric (Real-time karaoke subtitles)
      if (liveLyric != null && !liveLyric.isEmpty()) {
         String lyricClipped = clipText(font, "♪ " + liveLyric, WIDTH - textX - 8);
         g.text(font, Component.literal(lyricClipped), textX, 28, 0xFFFFD700);
      } else if (hasTrack) {
         String statusSub = "♫ " + formatTime(positionMs) + (durationMs > 0 ? " / " + formatTime(durationMs) : "");
         g.text(font, Component.literal(statusSub), textX, 28, 0xFF64748B);
      } else {
         g.text(font, Component.literal("♪ Music player ready"), textX, 28, 0xFF64748B);
      }

      // Line 4: Progress bar track
      int barY = 43;
      int barEnd = WIDTH - 48;
      g.fill(textX, barY, barEnd, barY + 3, 0x35FFFFFF);

      // Progress bar fill & knob
      if (durationMs > 0) {
         float progress = Math.max(0.0f, Math.min(1.0f, (float)positionMs / (float)durationMs));
         int fillW = (int)((barEnd - textX) * progress);
         if (fillW > 0) {
            g.fillGradient(textX, barY, textX + fillW, barY + 3, c1, c2);
            // Glowing knob head
            int knobX = textX + fillW;
            g.fill(knobX - 1, barY - 1, knobX + 2, barY + 4, 0xFFFFFFFF);
         }
      }

      // Time stamp on bottom right
      String timeText = formatTime(positionMs);
      if (durationMs > 0) {
         timeText = timeText + " / " + formatTime(durationMs);
      }
      g.text(font, Component.literal(timeText), WIDTH - 46, 39, 0xFF8E95AA);

      g.pose().popMatrix();
   }

   private static String clipText(Font font, String text, int maxWidth) {
      if (text == null || text.isEmpty()) return "";
      if (font.width(text) <= maxWidth) return text;
      String dots = "...";
      int dotsW = font.width(dots);
      int len = text.length();
      while (len > 1 && font.width(text.substring(0, len)) + dotsW > maxWidth) {
         len--;
      }
      return text.substring(0, len) + dots;
   }

   private static String formatTime(long ms) {
      long totalSec = Math.max(0L, ms / 1000L);
      long min = totalSec / 60L;
      long sec = totalSec % 60L;
      return String.format("%02d:%02d", min, sec);
   }
}
