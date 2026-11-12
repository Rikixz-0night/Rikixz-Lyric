package com.realtimelyrics.gui;

import java.util.function.Consumer;
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
public class VisualToggle extends AbstractWidget {
   private final String title;
   private final String description;
   private boolean state;
   private final Consumer<Boolean> onToggle;

   public VisualToggle(int x, int y, int w, int h, String title, String description, boolean initial, Consumer<Boolean> onToggle) {
      super(x, y, w, h, Component.literal(title));
      this.title = title;
      this.description = description;
      this.state = initial;
      this.onToggle = onToggle;
   }

   public boolean state() {
      return this.state;
   }

   public void setState(boolean s) {
      this.state = s;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.active && this.visible && event.button() == 0) {
         if (event.x() >= this.getX() && event.x() < this.getX() + this.getWidth()
             && event.y() >= this.getY() && event.y() < this.getY() + this.getHeight()) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.state = !this.state;
            if (this.onToggle != null) {
               this.onToggle.accept(this.state);
            }
            return true;
         }
      }
      return false;
   }

   @Override
   protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      boolean hovered = this.isHovered();
      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int h = this.getHeight();
      int radius = Math.max(5, Math.min(8, h / 2));

      int bg = hovered ? VisualTheme.CARD_HOVER_BG : VisualTheme.CARD_BG;
      int border = hovered ? VisualTheme.CARD_BORDER_HOVER : VisualTheme.CARD_BORDER;

      if (this.state) {
         bg = hovered ? 0xF01B2035 : 0xE0161B2E;
         border = hovered ? 0x88C4B5FD : 0x33C4B5FD;
         VisualTheme.fillRounded(g, x - 1, y - 1, w + 2, h + 2, radius + 1, 0x18C4B5FD);
      } else if (hovered) {
         VisualTheme.fillRounded(g, x - 1, y - 1, w + 2, h + 2, radius + 1, 0x12FFFFFF);
      }

      VisualTheme.drawRoundedPanel(g, x, y, w, h, radius, bg, border);

      // The active card gets a slim accent rail, echoing the website's highlight bars.
      if (this.state) {
         g.fill(x + 1, y + radius, x + 3, y + h - radius, VisualTheme.ACCENT_LAVENDER);
         g.fill(x + 3, y + radius, x + 4, y + h - radius, VisualTheme.ACCENT_MINT);
      }

      Font font = Minecraft.getInstance().font;
      int textX = x + (this.state ? 10 : 8);
      boolean hasDesc = this.description != null && !this.description.isEmpty();
      int titleY = hasDesc && h >= 20 ? y + 3 : y + (h - 8) / 2;

      int switchW = Math.max(26, Math.min(34, w / 5));
      int switchH = 14;
      int switchX = x + w - switchW - 8;
      int switchY = y + (h - switchH) / 2;
      int textMaxW = switchX - textX - 8;

      g.text(font, VisualTheme.clip(font, this.title, textMaxW), textX, titleY,
         this.state ? VisualTheme.TEXT_PRIMARY : VisualTheme.TEXT_SOFT);

      if (hasDesc && h >= 20) {
         String desc = VisualTheme.clip(font, this.description, textMaxW);
         g.text(font, desc, textX, y + 12, VisualTheme.TEXT_MUTED);
      }

      // Soft glass switch with a gradient "on" rail and a round knob.
      int trackBg = this.state ? 0xFF8B7CF6 : 0x66334155;
      VisualTheme.drawRoundedPanel(g, switchX, switchY, switchW, switchH, switchH / 2,
         trackBg, this.state ? 0x99C4B5FD : 0x33FFFFFF);

      if (this.state) {
         VisualTheme.drawHorizontalGradient(g, switchX + 2, switchY + 1, switchW - 4, switchH - 2,
            VisualTheme.ACCENT_VIOLET, VisualTheme.ACCENT_MINT);
         VisualTheme.fillRounded(g, switchX - 1, switchY - 1, switchW + 2, switchH + 2, switchH / 2 + 1,
            0x28C4B5FD);
      }

      int knobSize = switchH - 4;
      int knobX = this.state ? switchX + switchW - knobSize - 2 : switchX + 2;
      int knobY = switchY + 2;
      VisualTheme.fillCircle(g, knobX + knobSize / 2, knobY + knobSize / 2, knobSize / 2 + 1, 0x44000000);
      VisualTheme.drawRoundedPanel(g, knobX, knobY, knobSize, knobSize, knobSize / 2,
         this.state ? 0xFFFFFFFF : 0xFFB8C0D4, this.state ? 0xFFFFFFFF : 0x22FFFFFF);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
