package com.realtimelyrics.hud;

import com.realtimelyrics.LyricsConfig;
import com.realtimelyrics.LyricsController;
import com.realtimelyrics.gui.VisualButton;
import com.realtimelyrics.gui.VisualSlider;
import com.realtimelyrics.gui.VisualTheme;
import com.realtimelyrics.gui.VisualToggle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class HudPositionScreen extends Screen {
   private final Screen parent;
   private final LyricsConfig config;
   private final LyricsController controller;
   private final NowPlayingHud hud;
   private boolean isDragging;
   private double dragOffsetX;
   private double dragOffsetY;

   public HudPositionScreen(Screen parent, LyricsConfig config, LyricsController controller, NowPlayingHud hud) {
      super(Component.literal("♫ Song HUD Position"));
      this.parent = parent;
      this.config = config;
      this.controller = controller;
      this.hud = hud;
   }

   @Override
   protected void init() {
      this.clearWidgets();

      int btnY = this.height - 30;
      int btnW = 95;
      int gap = 6;
      int totalW = btnW * 4 + gap * 3;
      int startX = (this.width - totalW) / 2;

      // Bottom preset buttons with VisualTheme
      this.addRenderableWidget(new VisualButton(startX, btnY, btnW, 20, Component.literal("⌖ Center"), () -> {
         this.config.hudPercentX = 0.5;
         this.config.hudPercentY = 0.5;
         this.config.markDirty();
      }));

      this.addRenderableWidget(new VisualButton(startX + (btnW + gap), btnY, btnW, 20, Component.literal("⬆ Top Center"), () -> {
         this.config.hudPercentX = 0.5;
         this.config.hudPercentY = 0.08;
         this.config.markDirty();
      }));

      this.addRenderableWidget(new VisualButton(startX + (btnW + gap) * 2, btnY, btnW, 20, Component.literal("⬇ Bottom Center"), () -> {
         this.config.hudPercentX = 0.5;
         this.config.hudPercentY = 0.88;
         this.config.markDirty();
      }));

      this.addRenderableWidget(new VisualButton(startX + (btnW + gap) * 3, btnY, btnW, 20, Component.literal("✓ Save & Done"), this::onClose, VisualTheme.ACCENT_CYAN, true));

      // Top control bar
      int topBtnY = this.height - 56;
      int topTotalW = 145 * 2 + 8;
      int topStartX = (this.width - topTotalW) / 2;

      this.addRenderableWidget(new VisualSlider(topStartX, topBtnY, 145, 22, "HUD Scale", 0.5, 2.0, this.config.hudScale, v -> {
         this.config.hudScale = Math.round(v * 20.0) / 20.0;
         this.config.markDirty();
      }, v -> String.format("%.0f%%", v * 100.0)));

      this.addRenderableWidget(new VisualToggle(topStartX + 153, topBtnY, 145, 22, "Always Visible", "Show even when stopped", this.config.hudAlwaysShow, val -> {
         this.config.hudAlwaysShow = val;
         this.config.markDirty();
      }));
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0) {
         float scale = (float)this.config.hudScale;
         int w = (int)(NowPlayingHud.WIDTH * scale);
         int h = (int)(NowPlayingHud.HEIGHT * scale);
         int x = (int)(this.width * this.config.hudPercentX - w / 2.0f);
         int y = (int)(this.height * this.config.hudPercentY - h / 2.0f);

         if (event.x() >= x - 6 && event.x() <= x + w + 6 && event.y() >= y - 16 && event.y() <= y + h + 6) {
            this.isDragging = true;
            this.dragOffsetX = event.x() - x;
            this.dragOffsetY = event.y() - y;
            return true;
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.isDragging && event.button() == 0) {
         float scale = (float)this.config.hudScale;
         int w = (int)(NowPlayingHud.WIDTH * scale);
         int h = (int)(NowPlayingHud.HEIGHT * scale);

         double targetX = event.x() - this.dragOffsetX + w / 2.0;
         double targetY = event.y() - this.dragOffsetY + h / 2.0;

         // Magnetic snapping to horizontal or vertical center
         if (Math.abs(targetX / this.width - 0.5) < 0.025) {
            targetX = this.width * 0.5;
         }
         if (Math.abs(targetY / this.height - 0.5) < 0.025) {
            targetY = this.height * 0.5;
         }

         this.config.hudPercentX = Math.max(0.04, Math.min(0.96, targetX / this.width));
         this.config.hudPercentY = Math.max(0.04, Math.min(0.96, targetY / this.height));
         this.config.markDirty();
         return true;
      }
      return super.mouseDragged(event, dragX, dragY);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 0 && this.isDragging) {
         this.isDragging = false;
         this.config.save();
         return true;
      }
      return super.mouseReleased(event);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      // Dark translucent backdrop
      g.fill(0, 0, this.width, this.height, 0x66000000);

      // Header Banner
      VisualTheme.drawPanel(g, this.width / 2 - 130, 8, 260, 24, VisualTheme.MODAL_BG, VisualTheme.CARD_BORDER);
      g.centeredText(this.font, Component.literal("♫ Song HUD Position Editor").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.width / 2, 12, -1);
      g.centeredText(this.font, Component.literal("Click and drag the music card anywhere on screen").withStyle(ChatFormatting.DARK_GRAY), this.width / 2, 22, VisualTheme.TEXT_MUTED);

      float scale = (float)this.config.hudScale;
      int w = (int)(NowPlayingHud.WIDTH * scale);
      int h = (int)(NowPlayingHud.HEIGHT * scale);
      int x = (int)(this.width * this.config.hudPercentX - w / 2.0f);
      int y = (int)(this.height * this.config.hudPercentY - h / 2.0f);

      // Magnetic snapping crosshairs
      if (Math.abs(this.config.hudPercentX - 0.5) < 0.01) {
         g.verticalLine(this.width / 2, 0, this.height - 62, 0x5500F2FE);
      }
      if (Math.abs(this.config.hudPercentY - 0.5) < 0.01) {
         g.horizontalLine(0, this.width, this.height / 2, 0x5500F2FE);
      }

      // Render the HUD Card
      this.hud.renderHudCard(g, x, y, scale, this.isDragging, true);

      // Draggable boundary box
      int outlineColor = this.isDragging ? 0xFFFFD700 : 0xFF00F2FE;
      g.outline(x - 3, y - 3, w + 6, h + 6, outlineColor);

      // Drag tag handle
      String tag = this.isDragging ? "✦ Moving HUD..." : "✦ Drag to Move ✦";
      int tagColor = this.isDragging ? 0xFFFFD700 : 0xFFFFFFFF;
      g.centeredText(this.font, Component.literal(tag).withStyle(ChatFormatting.BOLD), x + w / 2, y - 12, tagColor);

      // Bottom bar backing panel
      int bottomPanelW = 95 * 4 + 6 * 3 + 16;
      int bottomPanelH = 60;
      int bottomPanelX = (this.width - bottomPanelW) / 2;
      int bottomPanelY = this.height - 62;
      VisualTheme.drawPanel(g, bottomPanelX, bottomPanelY, bottomPanelW, bottomPanelH, VisualTheme.MODAL_BG, VisualTheme.CARD_BORDER);

      super.extractRenderState(g, mouseX, mouseY, partialTick);
   }

   @Override
   public void removed() {
      this.config.save();
   }

   @Override
   public void onClose() {
      this.config.save();
      if (this.minecraft != null && this.minecraft.gui != null) {
         this.minecraft.gui.setScreen(this.parent);
      }
   }
}
