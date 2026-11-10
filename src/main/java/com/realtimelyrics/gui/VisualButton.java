package com.realtimelyrics.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class VisualButton extends AbstractWidget {
   private final Runnable action;
   private int accentColor;
   private boolean primary;

   public VisualButton(int x, int y, int w, int h, Component message, Runnable action) {
      this(x, y, w, h, message, action, VisualTheme.ACCENT_CYAN, false);
   }

   public VisualButton(int x, int y, int w, int h, Component message, Runnable action, int accentColor, boolean primary) {
      super(x, y, w, h, message);
      this.action = action;
      this.accentColor = accentColor;
      this.primary = primary;
   }

   public void setAccentColor(int color) {
      this.accentColor = color;
   }

   public void setPrimary(boolean primary) {
      this.primary = primary;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.active && this.visible && event.button() == 0) {
         if (event.x() >= this.getX() && event.x() < this.getX() + this.getWidth()
             && event.y() >= this.getY() && event.y() < this.getY() + this.getHeight()) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            if (this.action != null) {
               this.action.run();
            }
            return true;
         }
      }
      return false;
   }

   @Override
   protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      boolean hovered = this.isHovered();
      int bg;
      int border;
      int textColor;

      if (!this.active) {
         bg = 0x66111827;
         border = 0x22FFFFFF;
         textColor = VisualTheme.TEXT_DARK;
      } else if (this.primary) {
         bg = hovered ? 0xFF00D2FF : 0xEE0083B0;
         border = 0xFFFFFFFF;
         textColor = 0xFFFFFFFF;
      } else {
         bg = hovered ? VisualTheme.CARD_HOVER_BG : VisualTheme.CARD_BG;
         border = hovered ? this.accentColor : VisualTheme.CARD_BORDER;
         textColor = hovered ? 0xFFFFFFFF : VisualTheme.TEXT_PRIMARY;
      }

      VisualTheme.drawPanel(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), bg, border);

      if (hovered && this.active && !this.primary) {
         g.horizontalLine(this.getX() + 2, this.getX() + this.getWidth() - 3, this.getY() + 1, this.accentColor);
      }

      Font font = Minecraft.getInstance().font;
      int tx = this.getX() + this.getWidth() / 2;
      int ty = this.getY() + (this.getHeight() - 8) / 2;
      g.centeredText(font, this.getMessage(), tx, ty, textColor);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
