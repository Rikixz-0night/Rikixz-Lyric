package com.realtimelyrics.gui;

import com.realtimelyrics.render.Anim;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared visual language for the redesigned Rikixz Lyric interface.
 *
 * <p>The palette and glass panels mirror the public website: deep midnight
 * surfaces, warm white type, lavender highlights and mint accents.</p>
 */
@Environment(EnvType.CLIENT)
public final class VisualTheme {
   // Backdrop and surfaces
   public static final int BG_OVERLAY = 0xE8070A12;
   public static final int BG_DEEP = 0xFF070A12;
   public static final int BG_SOFT = 0xFF0B1020;
   public static final int MODAL_BG = 0xF70B1020;
   public static final int MODAL_BORDER = 0x38C4B5FD;
   public static final int HEADER_BG = 0xF50D1322;
   public static final int CARD_BG = 0xD8121828;
   public static final int CARD_HOVER_BG = 0xF01A2135;
   public static final int CARD_BORDER = 0x20FFFFFF;
   public static final int CARD_BORDER_HOVER = 0x70C4B5FD;
   public static final int CARD_FILL = 0xF00D1322;
   public static final int PANEL_INNER = 0x66000000;

   // Accents
   public static final int ACCENT_LAVENDER = 0xFFC4B5FD;
   public static final int ACCENT_LAVENDER_SOFT = 0x55C4B5FD;
   public static final int ACCENT_VIOLET = 0xFF9277F5;
   public static final int ACCENT_MINT = 0xFF99F6E4;
   public static final int ACCENT_MINT_SOFT = 0x5599F6E4;
   public static final int ACCENT_AMBER = 0xFFF5D9A8;
   public static final int ACCENT_PINK = 0xFFF3B6D0;
   public static final int ACCENT_RED = 0xFFFF8FA3;
   public static final int ACCENT_DARK = 0xFF0A0B13;

   // Backwards-compatible aliases used across the existing UI
   public static final int ACCENT_CYAN = ACCENT_MINT;
   public static final int ACCENT_BLUE = ACCENT_LAVENDER;
   public static final int ACCENT_GOLD = ACCENT_AMBER;
   public static final int ACCENT_GREEN = ACCENT_MINT;
   public static final int ACCENT_PURPLE = ACCENT_VIOLET;

   // Text
   public static final int TEXT_PRIMARY = 0xFFF1F3FA;
   public static final int TEXT_SOFT = 0xFFD2D8E6;
   public static final int TEXT_MUTED = 0xFFA3ABC0;
   public static final int TEXT_DARK = 0xFF737D98;
   public static final int TEXT_ON_LIGHT = 0xFF111320;

   private VisualTheme() {}

   public static int alpha(int color, int alpha) {
      return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
   }

   public static int rgba(int rgb, float alpha) {
      return Anim.argb(alpha, rgb);
   }

   /**
    * Draws a filled rectangle with softly stepped rounded corners.
    * Minecraft's GUI renderer has no anti-aliased primitives, so this keeps
    * the pixel-art feel while giving every surface a modern silhouette.
    */
   public static void fillRounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
      if (w <= 0 || h <= 0) {
         return;
      }

      int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
      if (r == 0) {
         g.fill(x, y, x + w, y + h, color);
         return;
      }

      g.fill(x, y + r, x + w, y + h - r, color);

      for (int i = 0; i < r; i++) {
         int dy = r - 1 - i;
         int inset = r - (int)Math.sqrt((double)(r * r - dy * dy));
         g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
         g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
      }
   }

   public static void drawRoundedPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int bg, int border) {
      if (w <= 0 || h <= 0) {
         return;
      }

      int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
      if (border != 0) {
         fillRounded(g, x, y, w, h, r, border);
         if (w > 2 && h > 2) {
            fillRounded(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), bg);
         }
      } else {
         fillRounded(g, x, y, w, h, r, bg);
      }
   }

   /**
    * Legacy panel helper. All existing widgets receive the new rounded look.
    */
   public static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int bg, int border) {
      drawRoundedPanel(g, x, y, w, h, 5, bg, border);
   }

   public static void drawGlassPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int border) {
      drawRoundedPanel(g, x, y, w, h, radius, CARD_BG, border);
      if (w > 2 && h > 2) {
         g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h / 2, 42), 0x18FFFFFF, 0x00FFFFFF);
         g.fill(x + radius, y + 1, x + w - radius, y + 2, 0x12FFFFFF);
      }
   }

   public static void drawGradientBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int color1, int color2) {
      g.fillGradient(x, y, x + w, y + 1, color1, color2);
      g.fillGradient(x, y + h - 1, x + w, y + h, color1, color2);
      g.fillGradient(x, y, x + 1, y + h, color1, color2);
      g.fillGradient(x + w - 1, y, x + w, y + h, color1, color2);
   }

   public static void drawHorizontalGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int color1, int color2) {
      if (w <= 0 || h <= 0) {
         return;
      }

      int steps = Math.max(1, Math.min(48, (w + 2) / 3));
      for (int i = 0; i < steps; i++) {
         int x1 = x + (w * i) / steps;
         int x2 = x + (w * (i + 1)) / steps;
         int rgb = Anim.mixRgb(color1, color2, steps <= 1 ? 0.0F : i / (float)(steps - 1));
         int alpha = (int)((((color1 >>> 24) & 0xFF) + ((color2 >>> 24) & 0xFF)) / 2.0F);
         g.fill(x1, y, x2, y + h, (alpha << 24) | rgb);
      }
   }

   public static void drawEqualizer(GuiGraphicsExtractor g, int x, int y, int w, int h, float time, int color) {
      drawEqualizer(g, x, y, w, h, time, color, color, true);
   }

   public static void drawEqualizer(
      GuiGraphicsExtractor g, int x, int y, int w, int h, float time, int color1, int color2, boolean playing
   ) {
      int bars = 5;
      int gap = 2;
      int barW = Math.max(1, (w - gap * (bars - 1)) / bars);
      int totalW = bars * barW + (bars - 1) * gap;

      for (int i = 0; i < bars; i++) {
         float wave = playing ? (float)Math.sin(time * 5.5F + i * 1.35F) : -0.5F;
         float factor = playing ? 0.28F + 0.72F * Math.abs(wave) : 0.22F + i * 0.035F;
         int barH = Math.max(2, (int)(h * factor));
         int bx = x + i * (barW + gap);
         int by = y + h - barH;
         int barColor = Anim.mixRgb(color1, color2, bars <= 1 ? 0.0F : i / (float)(bars - 1));
         g.fill(bx, by, bx + barW, by + barH, barColor | 0xFF000000);
      }

      // A tiny baseline grounds the bars without making the badge too heavy.
      g.fill(x, y + h, x + totalW, y + h + 1, alpha(color1, 0x30));
   }

   public static void fillCircle(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
      for (int dy = -radius; dy <= radius; dy += 2) {
         int dx = (int)Math.sqrt((double)(radius * radius - dy * dy));
         g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 2, color);
      }
   }

   /**
    * A screen-wide midnight backdrop with a soft aurora and a subtle star field.
    * It intentionally stays quiet so the actual controls remain the focus.
    */
   public static void drawBackdrop(GuiGraphicsExtractor g, int width, int height, float time) {
      g.fillGradient(0, 0, width, height, 0xFF090D18, BG_DEEP);

      drawGlow(g, (int)(width * 0.82), (int)(height * 0.10), Math.max(80, width / 4), ACCENT_VIOLET);
      drawGlow(g, (int)(width * 0.10), (int)(height * 0.88), Math.max(70, width / 5), ACCENT_MINT);

      // Deterministic stars keep the layout stable while still twinkling softly.
      int count = Math.max(26, Math.min(64, width / 15));
      for (int i = 0; i < count; i++) {
         int sx = (i * 137 + 43) % Math.max(1, width);
         int sy = (i * 83 + 29) % Math.max(1, height);
         float twinkle = 0.24F + 0.20F * (float)Math.sin(time * 1.3F + i * 0.71F);
         int size = i % 9 == 0 ? 2 : 1;
         g.fill(sx, sy, sx + size, sy + size, alpha(0xFFFFFF, (int)(twinkle * 255.0F)));
      }

      // Gentle vignette, built from edge bands rather than a texture.
      g.fillGradient(0, 0, width, Math.max(28, height / 8), 0x66000000, 0x00000000);
      g.fillGradient(0, height - Math.max(36, height / 6), width, height, 0x00000000, 0x88000000);
      g.fillGradient(0, 0, Math.max(24, width / 14), height, 0x33000000, 0x00000000);
      g.fillGradient(width - Math.max(24, width / 14), 0, width, height, 0x00000000, 0x33000000);
   }

   public static void drawGlow(GuiGraphicsExtractor g, int cx, int cy, int radius, int rgb) {
      if (radius <= 0) {
         return;
      }

      fillCircleLayer(g, cx, cy, radius, alpha(rgb, 10));
      fillCircleLayer(g, cx, cy, (int)(radius * 0.66F), alpha(rgb, 14));
      fillCircleLayer(g, cx, cy, (int)(radius * 0.36F), alpha(rgb, 18));
   }

   private static void fillCircleLayer(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
      for (int dy = -radius; dy <= radius; dy += 3) {
         int dx = (int)Math.sqrt((double)(radius * radius - dy * dy));
         g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 3, color);
      }
   }

   public static void drawStatusDot(GuiGraphicsExtractor g, int x, int y, int color, boolean pulse, float time) {
      int size = 3;
      if (pulse) {
         float glow = 0.35F + 0.30F * (float)Math.sin(time * 2.4F);
         fillCircle(g, x + size / 2, y + size / 2, 4, alpha(color, (int)(glow * 255.0F)));
      }
      g.fill(x, y, x + size, y + size, color);
   }

   public static void drawPill(
      GuiGraphicsExtractor g, Font font, int x, int y, String text, int textColor, int bg, int border
   ) {
      int w = font.width(text) + 10;
      drawRoundedPanel(g, x, y, w, 13, 6, bg, border);
      g.text(font, text, x + 5, y + 3, textColor);
   }

   public static void drawPill(
      GuiGraphicsExtractor g, Font font, int x, int y, String text, int maxWidth, int textColor, int bg, int border
   ) {
      String clipped = clip(font, text, Math.max(12, maxWidth - 10));
      int w = Math.min(maxWidth, font.width(clipped) + 10);
      drawRoundedPanel(g, x, y, w, 13, 6, bg, border);
      g.text(font, clipped, x + 5, y + 3, textColor);
   }

   /** Small mono-style section kicker with the short rule used on the website. */
   public static void drawKicker(GuiGraphicsExtractor g, Font font, int x, int y, String text, int color) {
      g.fill(x, y + 4, x + 14, y + 5, color);
      g.text(font, text, x + 20, y, color);
   }

   public static void drawCornerBrackets(
      GuiGraphicsExtractor g, int x, int y, int w, int h, int length, int color
   ) {
      int len = Math.max(2, Math.min(length, Math.min(w, h) / 2));
      g.fill(x, y, x + len, y + 2, color);
      g.fill(x, y, x + 2, y + len, color);
      g.fill(x + w - len, y, x + w, y + 2, color);
      g.fill(x + w - 2, y, x + w, y + len, color);
      g.fill(x, y + h - 2, x + len, y + h, color);
      g.fill(x, y + h - len, x + 2, y + h, color);
      g.fill(x + w - len, y + h - 2, x + w, y + h, color);
      g.fill(x + w - 2, y + h - len, x + w, y + h, color);
   }

   public static void drawDashedLine(
      GuiGraphicsExtractor g, boolean vertical, int x, int y, int length, int dash, int gap, int color
   ) {
      int step = Math.max(1, dash + gap);
      for (int i = 0; i < length; i += step) {
         int end = Math.min(length, i + dash);
         if (vertical) {
            g.fill(x, y + i, x + 1, y + end, color);
         } else {
            g.fill(x + i, y, x + end, y + 1, color);
         }
      }
   }

   public static void drawDotGrid(
      GuiGraphicsExtractor g, int x, int y, int w, int h, int spacing, int color
   ) {
      int step = Math.max(8, spacing);
      for (int gx = x; gx <= x + w; gx += step) {
         for (int gy = y; gy <= y + h; gy += step) {
            g.fill(gx, gy, gx + 1, gy + 1, color);
         }
      }
   }

   public static String clip(Font font, String text, int maxWidth) {
      if (text == null || text.isEmpty() || maxWidth <= 0) {
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
}
