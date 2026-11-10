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
      int bg = hovered ? VisualTheme.CARD_HOVER_BG : VisualTheme.CARD_BG;
      int border = hovered ? VisualTheme.CARD_BORDER_HOVER : VisualTheme.CARD_BORDER;

      VisualTheme.drawPanel(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), bg, border);

      Font font = Minecraft.getInstance().font;
      int textX = this.getX() + 8;
      int textY = this.getY() + 3;

      g.text(font, this.title, textX, textY, VisualTheme.TEXT_PRIMARY);

      if (this.description != null && !this.description.isEmpty()) {
         String sub = this.description;
         int maxW = this.getWidth() - 56;
         if (font.width(sub) > maxW) {
            sub = font.plainSubstrByWidth(sub, maxW - 6) + "...";
         }
         g.text(font, sub, textX, textY + 11, VisualTheme.TEXT_MUTED);
      }

      // Modern Pill switch on the right
      int pillW = 34;
      int pillH = 16;
      int pillX = this.getX() + this.getWidth() - pillW - 8;
      int pillY = this.getY() + (this.getHeight() - pillH) / 2;

      int pillBg = this.state ? 0xFF00C9FF : 0x66334155;
      int pillBorder = this.state ? 0xFF4FACFE : 0x4464748B;
      VisualTheme.drawPanel(g, pillX, pillY, pillW, pillH, pillBg, pillBorder);

      // Pill Knob
      int knobSize = 12;
      int knobY = pillY + 2;
      int knobX = this.state ? (pillX + pillW - knobSize - 2) : (pillX + 2);
      int knobColor = this.state ? 0xFFFFFFFF : 0xFF94A3B8;
      VisualTheme.drawPanel(g, knobX, knobY, knobSize, knobSize, knobColor, 0);

      // Status text
      if (this.state) {
         g.text(font, "ON", pillX + 4, pillY + 4, 0xFF003049);
      } else {
         g.text(font, "OFF", pillX + 14, pillY + 4, 0xFF64748B);
      }
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
