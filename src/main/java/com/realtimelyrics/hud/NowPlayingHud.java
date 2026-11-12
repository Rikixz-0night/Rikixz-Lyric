package com.realtimelyrics.hud;

import com.realtimelyrics.LyricsConfig;
import com.realtimelyrics.LyricsController;
import com.realtimelyrics.gui.VisualTheme;
import com.realtimelyrics.lyrics.Lyrics;
import com.realtimelyrics.media.MediaSample;
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
   public static final int WIDTH = 300;
   public static final int HEIGHT = 100;
   private static final int RADIUS = 14;
   private static final int CONTENT_X = 94;
   private static final int ART_SIZE = 68;
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

      String rawTitle = hasTrack ? this.controller.displayTitle() : (isPreview ? "Midnight Drive" : "Nothing playing");
      String rawArtist = hasTrack ? this.controller.displayArtist() : (isPreview ? "Rikixz · Neon Skyline" : "Open Spotify, YouTube, or another player");
      String rawAlbum = hasTrack && media.album() != null ? media.album() : (isPreview ? "Neon Skyline" : "");
      long durationMs = hasTrack ? media.durationMs() : (isPreview ? 228000L : 0L);
      long positionMs = hasTrack ? this.controller.songPositionMs() : (isPreview ? 84000L : 0L);
      boolean isPlaying = hasTrack ? this.controller.isPlaying() : isPreview;
      String app = hasTrack && media.app() != null && !media.app().isBlank()
         ? media.app().toUpperCase()
         : (isPreview ? "SPOTIFY" : "");

      String title = KhmerTextUtil.shape(rawTitle);
      String artist = KhmerTextUtil.shape(rawArtist);

      String subSongText = artist;
      if (!rawAlbum.isBlank() && !rawAlbum.equalsIgnoreCase(rawTitle)) {
         subSongText = artist + " · " + rawAlbum;
      }
      String subSong = KhmerTextUtil.shape(subSongText);

      String liveLyric = null;
      Lyrics lyrics = this.controller.lyrics();
      if (lyrics != null && !lyrics.lines().isEmpty()) {
         int idx = lyrics.indexAt(positionMs);
         if (idx >= 0 && idx < lyrics.lines().size()) {
            String line = lyrics.lines().get(idx).text();
            if (line != null && !line.isBlank()) {
               liveLyric = KhmerTextUtil.shape(line.trim());
            }
         }
      } else if (isPreview) {
         liveLyric = "We're running through the neon night";
      }

      int accent = hasTrack ? (isPlaying ? VisualTheme.ACCENT_LAVENDER : VisualTheme.ACCENT_AMBER) : VisualTheme.ACCENT_LAVENDER;
      int borderColor = isDragging
         ? 0xCCF5D9A8
         : (hasTrack ? (isPlaying ? 0x55C4B5FD : 0x44F5D9A8) : 0x24FFFFFF);
      float time = (System.currentTimeMillis() % 100000L) / 1000.0F;

      g.pose().pushMatrix();
      g.pose().translate((float)x, (float)y);
      g.pose().scale(scale, scale);

      // Ambient halo and soft drop shadow
      VisualTheme.fillRounded(g, -4, -4, WIDTH + 8, HEIGHT + 8, RADIUS + 4,
         VisualTheme.alpha(accent, isDragging ? 0x1E : 0x0E));
      VisualTheme.fillRounded(g, -2, -2, WIDTH + 4, HEIGHT + 4, RADIUS + 2,
         VisualTheme.alpha(accent, isDragging ? 0x2E : 0x16));
      VisualTheme.fillRounded(g, 1, 3, WIDTH, HEIGHT, RADIUS, 0x30000000);

      // Glass card body
      VisualTheme.drawRoundedPanel(g, 0, 0, WIDTH, HEIGHT, RADIUS, 0xF00B1020, borderColor);
      g.fillGradient(1 + RADIUS, 1, WIDTH - RADIUS - 1, 44, 0x18FFFFFF, 0x00FFFFFF);
      g.fill(1 + RADIUS, 1, WIDTH - RADIUS - 1, 2, 0x0CFFFFFF);
      VisualTheme.drawHorizontalGradient(g, RADIUS + 2, 1, WIDTH - RADIUS * 2 - 4, 1, 0x99C4B5FD, 0x9999F6E4);

      // Album artwork, with a soft rounded frame
      int artX = 12;
      int artY = 13;
      VisualTheme.fillRounded(g, artX - 2, artY - 1, ART_SIZE + 4, ART_SIZE + 4, 13, 0x28000000);
      VisualTheme.drawRoundedPanel(g, artX - 1, artY - 1, ART_SIZE + 2, ART_SIZE + 2, 11, 0xFF0A0F1B, 0x33FFFFFF);

      try {
         g.blit(LOGO_TEXTURE, artX, artY, artX + ART_SIZE, artY + ART_SIZE, 0.0F, 1.0F, 0.0F, 1.0F);
      } catch (Throwable ignored) {
         g.fill(artX, artY, artX + ART_SIZE, artY + ART_SIZE, 0xFF111827);
      }
      maskRoundedCorners(g, artX, artY, ART_SIZE, ART_SIZE, 9, 0xFF0A0F1B);

      int contentW = WIDTH - CONTENT_X - 13;
      String status = hasTrack ? (isPlaying ? "NOW PLAYING" : "PAUSED") : "PLAYER STANDBY";
      String meta = status + (app.isBlank() ? "" : "  ·  " + app);
      int metaColor = hasTrack ? (isPlaying ? VisualTheme.ACCENT_MINT : VisualTheme.ACCENT_AMBER) : VisualTheme.TEXT_DARK;

      VisualTheme.drawStatusDot(g, CONTENT_X, 17, metaColor, hasTrack && isPlaying, time);
      g.text(font, clipText(font, meta, contentW - 34), CONTENT_X + 7, 14, metaColor);

      VisualTheme.drawEqualizer(
         g, WIDTH - 13 - 24, 13, 24, 9, time,
         VisualTheme.ACCENT_LAVENDER, VisualTheme.ACCENT_MINT, hasTrack && isPlaying
      );

      // Song title, drawn slightly larger to create a clear hierarchy
      float titleScale = 1.2F;
      String titleClipped = clipText(font, title, (int)(contentW / titleScale));
      g.pose().pushMatrix();
      g.pose().translate((float)CONTENT_X, 24.0F);
      g.pose().scale(titleScale, titleScale);
      g.text(font, Component.literal(titleClipped).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF, true);
      g.pose().popMatrix();

      String subSongClipped = clipText(font, subSong, contentW);
      g.text(font, subSongClipped, CONTENT_X, 40, 0xFFB4BCCF);

      // Live karaoke line
      if (liveLyric != null && !liveLyric.isEmpty()) {
         g.text(font, "♪", CONTENT_X, 53, VisualTheme.ACCENT_MINT);
         g.text(font, clipText(font, liveLyric, contentW - 10), CONTENT_X + 10, 53, 0xFFDDD7FF, true);
      } else if (hasTrack) {
         String statusSub = "♫  " + formatTime(positionMs) + (durationMs > 0 ? " / " + formatTime(durationMs) : "");
         g.text(font, statusSub, CONTENT_X, 53, VisualTheme.TEXT_DARK);
      } else {
         g.text(font, "♪  Your lyrics will appear here", CONTENT_X, 53, VisualTheme.TEXT_DARK);
      }

      // Progress rail
      int barY = 74;
      int barH = 4;
      VisualTheme.fillRounded(g, CONTENT_X, barY, contentW, barH, 2, 0x2EFFFFFF);

      if (durationMs > 0) {
         float progress = Math.max(0.0F, Math.min(1.0F, (float)positionMs / (float)durationMs));
         int fillW = (int)(contentW * progress);
         if (fillW > 0) {
            VisualTheme.fillRounded(g, CONTENT_X - 1, barY - 1, fillW + 2, barH + 2, 3, 0x18C4B5FD);
            VisualTheme.drawHorizontalGradient(g, CONTENT_X, barY, Math.max(2, fillW), barH,
               VisualTheme.ACCENT_LAVENDER, VisualTheme.ACCENT_MINT);
         }

         g.text(font, formatTime(positionMs), CONTENT_X, 82, VisualTheme.TEXT_DARK);
         String total = formatTime(durationMs);
         g.text(font, total, CONTENT_X + contentW - font.width(total), 82, VisualTheme.TEXT_DARK);
      } else if (isPreview) {
         VisualTheme.drawHorizontalGradient(g, CONTENT_X, barY, (int)(contentW * 0.37F), barH,
            VisualTheme.ACCENT_LAVENDER, VisualTheme.ACCENT_MINT);
         g.text(font, "01:24", CONTENT_X, 82, VisualTheme.TEXT_DARK);
         String total = "03:48";
         g.text(font, total, CONTENT_X + contentW - font.width(total), 82, VisualTheme.TEXT_DARK);
      }

      g.pose().popMatrix();
   }

   private static void maskRoundedCorners(
      GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color
   ) {
      for (int i = 0; i < radius; i++) {
         int dy = radius - 1 - i;
         int inset = radius - (int)Math.sqrt((double)(radius * radius - dy * dy));
         if (inset <= 0) {
            continue;
         }
         g.fill(x, y + i, x + inset, y + i + 1, color);
         g.fill(x + w - inset, y + i, x + w, y + i + 1, color);
         g.fill(x, y + h - 1 - i, x + inset, y + h - i, color);
         g.fill(x + w - inset, y + h - 1 - i, x + w, y + h - i, color);
      }
   }

   private static String clipText(Font font, String text, int maxWidth) {
      if (text == null || text.isEmpty()) {
         return "";
      }
      if (font.width(text) <= maxWidth) {
         return text;
      }

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
      return String.format("%d:%02d", min, sec);
   }
}
