package com.realtimelyrics.gui;

import java.util.function.Consumer;
import java.util.function.DoubleFunction;
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
public class VisualSlider extends AbstractWidget {
   private final String label;
   private final double min;
   private final double max;
   private double value;
   private final Consumer<Double> onValueChange;
   private final DoubleFunction<String> formatter;
   private boolean isSliding;

   public VisualSlider(
      int x, int y, int w, int h,
      String label, double min, double max, double value,
      Consumer<Double> onValueChange, DoubleFunction<String> formatter
   ) {
      super(x, y, w, h, Component.literal(label));
      this.label = label;
      this.min = min;
      this.max = max;
      this.value = Math.max(min, Math.min(max, value));
      this.onValueChange = onValueChange;
      this.formatter = formatter;
   }

   public double getValue() {
      return this.value;
   }

   public void setValue(double val) {
      this.value = Math.max(this.min, Math.min(this.max, val));
   }

   private double normalized() {
      if (this.max <= this.min) return 0.0;
      return Math.max(0.0, Math.min(1.0, (this.value - this.min) / (this.max - this.min)));
   }

   private void updateFromMouse(double mouseX) {
      int trackX = this.getX() + 8;
      int trackW = this.getWidth() - 16;
      if (trackW <= 0) return;
      double norm = Math.max(0.0, Math.min(1.0, (mouseX - trackX) / (double)trackW));
      this.value = this.min + norm * (this.max - this.min);
      if (this.onValueChange != null) {
         this.onValueChange.accept(this.value);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.active && this.visible && event.button() == 0) {
         if (event.x() >= this.getX() && event.x() < this.getX() + this.getWidth()
             && event.y() >= this.getY() && event.y() < this.getY() + this.getHeight()) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.isSliding = true;
            this.updateFromMouse(event.x());
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.isSliding && event.button() == 0) {
         this.updateFromMouse(event.x());
         return true;
      }
      return super.mouseDragged(event, dragX, dragY);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 0 && this.isSliding) {
         this.isSliding = false;
         return true;
      }
      return super.mouseReleased(event);
   }

   @Override
   protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      boolean hovered = this.isHovered() || this.isSliding;
      int bg = hovered ? VisualTheme.CARD_HOVER_BG : VisualTheme.CARD_BG;
      int border = hovered ? VisualTheme.CARD_BORDER_HOVER : VisualTheme.CARD_BORDER;

      VisualTheme.drawPanel(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), bg, border);

      Font font = Minecraft.getInstance().font;

      // Label on top left
      g.text(font, this.label, this.getX() + 8, this.getY() + 4, VisualTheme.TEXT_PRIMARY);

      // Formatted Value on top right
      String valStr = this.formatter.apply(this.value);
      int valW = font.width(valStr);
      g.text(font, valStr, this.getX() + this.getWidth() - valW - 8, this.getY() + 4, VisualTheme.ACCENT_CYAN);

      // Slider Track Bar
      int trackX = this.getX() + 8;
      int trackY = this.getY() + this.getHeight() - 8;
      int trackW = this.getWidth() - 16;
      int trackH = 4;

      // Background track
      g.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0x66334155);

      // Gradient filled active track
      int fillW = (int)(trackW * this.normalized());
      if (fillW > 0) {
         g.fillGradient(trackX, trackY, trackX + fillW, trackY + trackH, 0xFF00C9FF, 0xFF92FE9D);
      }

      // Slider Thumb Knob
      int knobX = trackX + fillW;
      int knobY = trackY + trackH / 2;
      int knobR = hovered ? 4 : 3;
      int knobColor = hovered ? 0xFFFFFFFF : 0xFF00F2FE;

      VisualTheme.drawPanel(g, knobX - knobR, knobY - knobR, knobR * 2 + 1, knobR * 2 + 1, knobColor, 0xFF0083B0);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
