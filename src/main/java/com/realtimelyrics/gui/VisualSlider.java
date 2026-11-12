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
      if (this.max <= this.min) {
         return 0.0;
      }
      return Math.max(0.0, Math.min(1.0, (this.value - this.min) / (this.max - this.min)));
   }

   private void updateFromMouse(double mouseX) {
      int trackX = this.getX() + 9;
      int trackW = this.getWidth() - 18;
      if (trackW <= 0) {
         return;
      }
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
      int bg = hovered ? VisualTheme.CARD_HOVER_BG : 0xD8121828;
      int border = hovered ? VisualTheme.ACCENT_LAVENDER : VisualTheme.CARD_BORDER;
      int radius = Math.max(5, Math.min(8, this.getHeight() / 2));

      VisualTheme.drawRoundedPanel(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), radius, bg, border);
      if (this.getHeight() > 6) {
         g.fillGradient(this.getX() + radius, this.getY() + 1,
            this.getX() + this.getWidth() - radius, this.getY() + Math.max(2, this.getHeight() / 2),
            0x12FFFFFF, 0x00FFFFFF);
      }

      Font font = Minecraft.getInstance().font;
      g.text(font, this.label, this.getX() + 9, this.getY() + 4, VisualTheme.TEXT_SOFT);

      String valStr = this.formatter.apply(this.value);
      int valW = font.width(valStr);
      g.text(font, valStr, this.getX() + this.getWidth() - valW - 9, this.getY() + 4, VisualTheme.ACCENT_LAVENDER);

      int trackX = this.getX() + 9;
      int trackY = this.getY() + this.getHeight() - 7;
      int trackW = this.getWidth() - 18;
      int trackH = 3;

      VisualTheme.fillRounded(g, trackX, trackY, trackW, trackH, 2, 0x3AFFFFFF);

      int fillW = (int)(trackW * this.normalized());
      if (fillW > 0) {
         VisualTheme.drawHorizontalGradient(g, trackX, trackY, Math.max(2, fillW), trackH,
            VisualTheme.ACCENT_LAVENDER, VisualTheme.ACCENT_MINT);
      }

      int knobX = trackX + fillW;
      int knobY = trackY + trackH / 2;
      VisualTheme.fillCircle(g, knobX, knobY, hovered ? 5 : 4, VisualTheme.alpha(VisualTheme.ACCENT_LAVENDER, 0x35));
      int knobR = hovered ? 2 : 2;
      VisualTheme.fillCircle(g, knobX, knobY, knobR, hovered ? 0xFFFFFFFF : 0xFFE9E4FF);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
