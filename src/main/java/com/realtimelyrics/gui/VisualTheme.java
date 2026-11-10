package com.realtimelyrics.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Environment(EnvType.CLIENT)
public final class VisualTheme {
   public static final int BG_OVERLAY = 0xC806090F;
   public static final int MODAL_BG = 0xF20D131F;
   public static final int MODAL_BORDER = 0x5500F2FE;
   public static final int HEADER_BG = 0xF5111927;
   public static final int CARD_BG = 0xD8162032;
   public static final int CARD_HOVER_BG = 0xF01E2B42;
   public static final int CARD_BORDER = 0x334FACFE;
   public static final int CARD_BORDER_HOVER = 0xAA00F2FE;

   public static final int ACCENT_CYAN = 0xFF00F2FE;
   public static final int ACCENT_BLUE = 0xFF4FACFE;
   public static final int ACCENT_GOLD = 0xFFFFD700;
   public static final int ACCENT_GREEN = 0xFF00E676;
   public static final int ACCENT_PURPLE = 0xFF9B51E0;
   public static final int ACCENT_RED = 0xFFFF5252;

   public static final int TEXT_PRIMARY = 0xFFF8FAFC;
   public static final int TEXT_MUTED = 0xFF94A3B8;
   public static final int TEXT_DARK = 0xFF64748B;

   private VisualTheme() {}

   public static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int bg, int border) {
      if (w > 2 && h > 2) {
         g.fill(x + 1, y, x + w - 1, y + h, bg);
         g.fill(x, y + 1, x + 1, y + h - 1, bg);
         g.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);

         if (border != 0) {
            g.horizontalLine(x + 1, x + w - 2, y, border);
            g.horizontalLine(x + 1, x + w - 2, y + h - 1, border);
            g.verticalLine(x, y + 1, y + h - 2, border);
            g.verticalLine(x + w - 1, y + 1, y + h - 2, border);
         }
      } else {
         g.fill(x, y, x + w, y + h, bg);
      }
   }

   public static void drawGradientBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int color1, int color2) {
      g.fillGradient(x, y, x + w, y + 1, color1, color2);
      g.fillGradient(x, y + h - 1, x + w, y + h, color1, color2);
      g.fillGradient(x, y, x + 1, y + h, color1, color2);
      g.fillGradient(x + w - 1, y, x + w, y + h, color1, color2);
   }

   public static void drawEqualizer(GuiGraphicsExtractor g, int x, int y, int w, int h, float time, int color) {
      int bars = 5;
      int gap = 2;
      int barW = Math.max(2, (w - gap * (bars - 1)) / bars);
      for (int i = 0; i < bars; i++) {
         float wave = (float)Math.sin(time * 6.0F + i * 1.3F);
         float heightFactor = 0.3F + 0.7F * Math.abs(wave);
         int barH = Math.max(2, (int)(h * heightFactor));
         int bx = x + i * (barW + gap);
         int by = y + (h - barH);
         g.fill(bx, by, bx + barW, by + barH, color);
      }
   }
}
