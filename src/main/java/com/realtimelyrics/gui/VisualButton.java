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
      this(x, y, w, h, message, action, VisualTheme.ACCENT_LAVENDER, false);
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
      int radius = Math.max(5, Math.min(8, this.getHeight() / 2));
      int bg;
      int border;
      int textColor;

      if (!this.active) {
         bg = 0x66111827;
         border = 0x20FFFFFF;
         textColor = VisualTheme.TEXT_DARK;
      } else if (this.primary) {
         bg = hovered ? 0xFFFFFFFF : 0xFFE9E4FF;
         border = hovered ? 0xFFFFFFFF : 0x55FFFFFF;
         textColor = VisualTheme.TEXT_ON_LIGHT;
      } else {
         bg = hovered ? VisualTheme.CARD_HOVER_BG : 0xD8121828;
         border = hovered ? this.accentColor : VisualTheme.CARD_BORDER;
         textColor = hovered ? VisualTheme.TEXT_PRIMARY : VisualTheme.TEXT_SOFT;
      }

      if (this.active && hovered) {
         VisualTheme.fillRounded(g, this.getX() - 1, this.getY() - 1,
            this.getWidth() + 2, this.getHeight() + 2, radius + 1,
            VisualTheme.alpha(this.primary ? VisualTheme.ACCENT_LAVENDER : this.accentColor, 0x28));
      }

      VisualTheme.drawRoundedPanel(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(),
         radius, bg, border);

      if (this.active && !this.primary) {
         g.fillGradient(this.getX() + radius, this.getY() + 1,
            this.getX() + this.getWidth() - radius, this.getY() + Math.max(2, this.getHeight() / 2),
            hovered ? 0x18FFFFFF : 0x0CFFFFFF, 0x00FFFFFF);
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
