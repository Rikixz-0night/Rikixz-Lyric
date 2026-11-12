package com.realtimelyrics.hud;

import com.realtimelyrics.LyricsConfig;
import com.realtimelyrics.LyricsController;
import com.realtimelyrics.gui.VisualButton;
import com.realtimelyrics.gui.VisualSlider;
import com.realtimelyrics.gui.VisualTheme;
import com.realtimelyrics.gui.VisualToggle;
import com.realtimelyrics.media.MediaSample;
import com.realtimelyrics.render.KhmerTextUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class HudPositionScreen extends Screen {
   private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("rikixz_lyric", "textures/gui/logo.png");
   private static final int SHELF_PAD = 8;
   private static final int SHELF_ROW_H = 20;
   private static final int SHELF_GAP = 5;

   private final Screen parent;
   private final LyricsConfig config;
   private final LyricsController controller;
   private final NowPlayingHud hud;
   private boolean isDragging;
   private double dragOffsetX;
   private double dragOffsetY;

   private int headerX;
   private int headerY;
   private int headerW;
   private int headerH;
   private int shelfX;
   private int shelfY;
   private int shelfW;
   private int shelfH;
   private int stageTop;
   private int stageBottom;

   public HudPositionScreen(Screen parent, LyricsConfig config, LyricsController controller, NowPlayingHud hud) {
      super(Component.literal("Rikixz Lyric · HUD Placement"));
      this.parent = parent;
      this.config = config;
      this.controller = controller;
      this.hud = hud;
   }

   private void layout() {
      int margin = Math.max(6, Math.min(10, this.width / 70));
      this.headerX = margin;
      this.headerY = margin;
      this.headerW = this.width - margin * 2;
      this.headerH = this.height < 260 ? 32 : 36;

      this.shelfW = Math.min(this.width - margin * 2, 620);
      this.shelfH = SHELF_PAD * 2 + SHELF_ROW_H * 2 + SHELF_GAP;
      this.shelfX = (this.width - this.shelfW) / 2;
      this.shelfY = this.height - this.shelfH - margin;

      this.stageTop = this.headerY + this.headerH + 6;
      this.stageBottom = this.shelfY - 12;
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.layout();

      int innerX = this.shelfX + SHELF_PAD;
      int innerW = this.shelfW - SHELF_PAD * 2;
      int row1Y = this.shelfY + SHELF_PAD;
      int row2Y = row1Y + SHELF_ROW_H + SHELF_GAP;

      int toggleW = Math.max(118, Math.min(190, innerW * 38 / 100));
      int sliderW = innerW - toggleW - 6;

      this.addRenderableWidget(new VisualSlider(innerX, row1Y, sliderW, SHELF_ROW_H, "HUD Scale", 0.5, 2.0, this.config.hudScale, v -> {
         this.config.hudScale = Math.round(v * 20.0) / 20.0;
         this.config.markDirty();
      }, v -> String.format("%.0f%%", Math.round(v * 20.0) * 5.0)));

      this.addRenderableWidget(new VisualToggle(innerX + sliderW + 6, row1Y, toggleW, SHELF_ROW_H,
         "Always Visible", "Show while stopped", this.config.hudAlwaysShow, val -> {
            this.config.hudAlwaysShow = val;
            this.config.markDirty();
         }));

      int doneW = Math.max(68, Math.min(92, innerW / 5));
      int presetGap = 5;
      int presetW = (innerW - doneW - presetGap * 3) / 3;

      this.addRenderableWidget(new VisualButton(innerX, row2Y, presetW, SHELF_ROW_H, Component.literal("Center"), () -> {
         this.config.hudPercentX = 0.5;
         this.config.hudPercentY = 0.5;
         this.config.markDirty();
      }, VisualTheme.ACCENT_LAVENDER, false));

      this.addRenderableWidget(new VisualButton(innerX + presetW + presetGap, row2Y, presetW, SHELF_ROW_H,
         Component.literal("Top"), () -> {
            this.config.hudPercentX = 0.5;
            this.config.hudPercentY = this.topPresetPercent();
            this.config.markDirty();
         }, VisualTheme.ACCENT_LAVENDER, false));

      this.addRenderableWidget(new VisualButton(innerX + (presetW + presetGap) * 2, row2Y, presetW, SHELF_ROW_H,
         Component.literal("Bottom"), () -> {
            this.config.hudPercentX = 0.5;
            this.config.hudPercentY = this.bottomPresetPercent();
            this.config.markDirty();
         }, VisualTheme.ACCENT_LAVENDER, false));

      this.addRenderableWidget(new VisualButton(innerX + innerW - doneW, row2Y, doneW, SHELF_ROW_H,
         Component.literal("Done"), this::onClose, VisualTheme.ACCENT_MINT, true));
   }

   private float topPresetPercent() {
      int cardH = (int)(NowPlayingHud.HEIGHT * (float)this.config.hudScale);
      if (cardH >= this.height - 4) {
         return 0.5F;
      }
      return clampPercent((6.0F + cardH / 2.0F) / this.height);
   }

   private float bottomPresetPercent() {
      int cardH = (int)(NowPlayingHud.HEIGHT * (float)this.config.hudScale);
      if (cardH >= this.height - 4) {
         return 0.5F;
      }
      return clampPercent((this.height - 6.0F - cardH / 2.0F) / this.height);
   }

   private static float clampPercent(double value) {
      return (float)Math.max(0.02, Math.min(0.98, value));
   }

   private int cardX() {
      float scale = (float)this.config.hudScale;
      return (int)(this.width * this.config.hudPercentX - NowPlayingHud.WIDTH * scale / 2.0F);
   }

   private int cardY() {
      float scale = (float)this.config.hudScale;
      return (int)(this.height * this.config.hudPercentY - NowPlayingHud.HEIGHT * scale / 2.0F);
   }

   private int cardW() {
      return (int)(NowPlayingHud.WIDTH * (float)this.config.hudScale);
   }

   private int cardH() {
      return (int)(NowPlayingHud.HEIGHT * (float)this.config.hudScale);
   }

   private static boolean inside(double px, double py, int x, int y, int w, int h) {
      return px >= x && px < x + w && py >= y && py < y + h;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0) {
         // The chrome stays fully interactive even when the card is dragged underneath it.
         if (inside(event.x(), event.y(), this.headerX, this.headerY, this.headerW, this.headerH)
             || inside(event.x(), event.y(), this.shelfX, this.shelfY, this.shelfW, this.shelfH)) {
            return super.mouseClicked(event, doubleClick);
         }

         int x = this.cardX();
         int y = this.cardY();
         int w = this.cardW();
         int h = this.cardH();
         if (inside(event.x(), event.y(), x - 5, y - 5, w + 10, h + 10)) {
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

         double minX = 4.0 + w / 2.0;
         double maxX = this.width - 4.0 - w / 2.0;
         double minY = 4.0 + h / 2.0;
         double maxY = this.height - 4.0 - h / 2.0;

         targetX = maxX < minX ? this.width / 2.0 : Math.max(minX, Math.min(maxX, targetX));
         targetY = maxY < minY ? this.height / 2.0 : Math.max(minY, Math.min(maxY, targetY));

         // Magnetic snapping to the exact screen axes.
         if (Math.abs(targetX - this.width / 2.0) < 7.0) {
            targetX = this.width / 2.0;
         }
         if (maxY >= minY && Math.abs(targetY - this.height / 2.0) < 7.0) {
            targetY = this.height / 2.0;
         }

         this.config.hudPercentX = Math.max(0.02, Math.min(0.98, targetX / this.width));
         this.config.hudPercentY = Math.max(0.02, Math.min(0.98, targetY / this.height));
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
      this.layout();

      float time = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
      VisualTheme.drawBackdrop(g, this.width, this.height, time);

      // Stage texture and snap guides
      VisualTheme.drawDotGrid(g, this.headerX + 4, this.stageTop, this.width - (this.headerX + 4) * 2,
         Math.max(1, this.stageBottom - this.stageTop), 24, 0x12FFFFFF);
      VisualTheme.drawDashedLine(g, true, this.width / 2, this.stageTop,
         Math.max(0, this.stageBottom - this.stageTop), 4, 6, 0x33C4B5FD);

      int centerY = this.height / 2;
      if (centerY >= this.stageTop && centerY <= this.stageBottom) {
         VisualTheme.drawDashedLine(g, false, 0, centerY, this.width, 4, 6, 0x26C4B5FD);
      }

      g.fill(this.headerX, this.stageTop - 3, this.width - this.headerX, this.stageTop - 2, 0x18FFFFFF);

      // Card, selection brackets, and the floating drag handle
      int x = this.cardX();
      int y = this.cardY();
      int w = this.cardW();
      int h = this.cardH();
      boolean hovered = inside(mouseX, mouseY, x, y, w, h);

      int accent = this.isDragging ? VisualTheme.ACCENT_AMBER : VisualTheme.ACCENT_MINT;
      VisualTheme.fillRounded(g, x - 5, y - 5, w + 10, h + 10, 18,
         VisualTheme.alpha(accent, this.isDragging ? 0x2E : (hovered ? 0x1C : 0x10)));

      this.hud.renderHudCard(g, x, y, (float)this.config.hudScale, this.isDragging, true);

      VisualTheme.drawCornerBrackets(g, x - 6, y - 6, w + 12, h + 12, 14,
         VisualTheme.alpha(accent, this.isDragging ? 0xFF : 0xB0));

      String tag = this.isDragging
         ? String.format("Moving · %.0f%% / %.0f%%", this.config.hudPercentX * 100.0, this.config.hudPercentY * 100.0)
         : "Drag to reposition";
      int tagW = this.font.width(tag) + 20;
      int tagX = x + w / 2 - tagW / 2;
      int tagY = y - 22;
      if (tagY < this.stageTop + 2) {
         int below = y + h + 6;
         tagY = below + 14 <= this.stageBottom ? below : y + 4;
      }
      tagX = Math.max(4, Math.min(this.width - tagW - 4, tagX));

      VisualTheme.drawRoundedPanel(g, tagX, tagY, tagW, 15, 7,
         this.isDragging ? 0xE6121828 : 0xD80A0F1B,
         this.isDragging ? 0x99F5D9A8 : 0x33FFFFFF);
      g.fill(tagX + 6, tagY + 6, tagX + 9, tagY + 9, accent);
      g.text(this.font, tag, tagX + 13, tagY + 4, this.isDragging ? VisualTheme.ACCENT_AMBER : VisualTheme.TEXT_SOFT);

      // Chrome is painted above the card so the safe area reads clearly.
      this.drawHeader(g, time);
      this.drawShelf(g);

      String readout = "X " + Math.round(this.config.hudPercentX * 100.0) + "%   ·   Y "
         + Math.round(this.config.hudPercentY * 100.0) + "%";
      g.text(this.font, readout, this.shelfX + this.shelfW - this.font.width(readout) - 2,
         this.shelfY - 11, VisualTheme.TEXT_DARK);
      VisualTheme.drawKicker(g, this.font, this.shelfX + 2, this.shelfY - 11,
         "PLACEMENT", VisualTheme.TEXT_DARK);

      super.extractRenderState(g, mouseX, mouseY, partialTick);
   }

   private void drawHeader(GuiGraphicsExtractor g, float time) {
      VisualTheme.drawRoundedPanel(g, this.headerX, this.headerY, this.headerW, this.headerH, 11,
         0xE60B1020, 0x33FFFFFF);
      VisualTheme.drawHorizontalGradient(g, this.headerX + 22, this.headerY + 1,
         Math.max(4, this.headerW - 44), 1, 0x99C4B5FD, 0x8899F6E4);

      int mark = this.headerH - 12;
      int markX = this.headerX + 7;
      int markY = this.headerY + 6;
      VisualTheme.drawRoundedPanel(g, markX, markY, mark, mark, 6, 0x1AFFFFFF, 0x44C4B5FD);
      try {
         g.blit(LOGO_TEXTURE, markX + 1, markY + 1, markX + mark - 1, markY + mark - 1, 0.0F, 1.0F, 0.0F, 1.0F);
      } catch (Throwable ignored) {
         g.centeredText(this.font, "♪", markX + mark / 2, markY + mark / 2 - 4, VisualTheme.ACCENT_MINT);
      }

      int textX = markX + mark + 8;
      int textY = this.headerY + this.headerH / 2 - 4;
      g.text(this.font, Component.literal("Rikixz Lyric").withStyle(ChatFormatting.BOLD), textX, textY, 0xFFFFFFFF);

      MediaSample media = this.controller != null ? this.controller.currentMedia() : MediaSample.none();
      boolean hasTrack = media.hasTrack();
      boolean playing = hasTrack && (this.controller == null || this.controller.isPlaying());
      String app = hasTrack && media.app() != null && !media.app().isBlank() ? media.app().toUpperCase() : "";
      String status = hasTrack
         ? (playing ? "NOW PLAYING" : "PAUSED") + (app.isBlank() ? "" : "  ·  " + app)
         : "STANDBY";
      String title = hasTrack ? "  ·  " + KhmerTextUtil.shape(media.title()) : "";

      int maxPill = Math.min(230, this.headerW / 2);
      String pillText = VisualTheme.clip(this.font, status + title, maxPill - 24);
      int pillW = Math.min(maxPill, this.font.width(pillText) + 24);
      int pillX = this.headerX + this.headerW - pillW - 7;
      int pillY = this.headerY + (this.headerH - 18) / 2;
      int statusColor = hasTrack ? (playing ? VisualTheme.ACCENT_MINT : VisualTheme.ACCENT_AMBER) : VisualTheme.TEXT_MUTED;

      int brandW = this.font.width("Rikixz Lyric");
      int subX = textX + brandW + 7;
      int subSpace = pillX - subX - 10;
      if (subSpace > 70) {
         g.text(this.font, VisualTheme.clip(this.font, "·  HUD Placement", subSpace), subX, textY, VisualTheme.TEXT_MUTED);
      }

      VisualTheme.drawRoundedPanel(g, pillX, pillY, pillW, 18, 8, 0x66000000, 0x2AFFFFFF);
      if (playing) {
         VisualTheme.drawEqualizer(g, pillX + 7, pillY + 5, 12, 8, time, VisualTheme.ACCENT_LAVENDER, VisualTheme.ACCENT_MINT, true);
      } else {
         VisualTheme.drawStatusDot(g, pillX + 8, pillY + 7, statusColor, false, time);
      }
      g.text(this.font, pillText, pillX + 22, pillY + 5, statusColor);

      if (this.headerW >= 520) {
         int hintW = this.font.width("[ESC] Back");
         int hintX = pillX - hintW - 14;
         g.text(this.font, "[ESC] Back", hintX, textY, VisualTheme.TEXT_DARK);
      }
   }

   private void drawShelf(GuiGraphicsExtractor g) {
      VisualTheme.drawRoundedPanel(g, this.shelfX, this.shelfY, this.shelfW, this.shelfH, 11,
         0xEB0B1020, 0x33FFFFFF);
      VisualTheme.drawHorizontalGradient(g, this.shelfX + 22, this.shelfY + 1,
         Math.max(4, this.shelfW - 44), 1, 0x8899F6E4, 0x99C4B5FD);

      int row2Y = this.shelfY + SHELF_PAD + SHELF_ROW_H + SHELF_GAP;
      g.fill(this.shelfX + 8, row2Y - 3, this.shelfX + this.shelfW - 8, row2Y - 2, 0x12FFFFFF);
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
