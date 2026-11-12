package com.realtimelyrics;

import com.realtimelyrics.gui.VisualButton;
import com.realtimelyrics.gui.VisualSlider;
import com.realtimelyrics.gui.VisualTheme;
import com.realtimelyrics.gui.VisualToggle;
import com.realtimelyrics.hud.HudPositionScreen;
import com.realtimelyrics.hud.NowPlayingHud;
import com.realtimelyrics.media.MediaSample;
import com.realtimelyrics.render.Anim;
import com.realtimelyrics.render.KhmerTextUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class LyricsConfigScreen extends Screen {
   private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("rikixz_lyric", "textures/gui/logo.png");

   public enum Tab {
      SKY("Sky"),
      HUD("Song HUD"),
      EFFECTS("Visual FX"),
      SYNC("Sync");

      final String label;
      Tab(String label) { this.label = label; }
   }

   private final Screen parent;
   private final LyricsConfig config;
   private final LyricsController controller;
   private final NowPlayingHud hud;
   private Tab activeTab = Tab.SKY;

   private int winX;
   private int winY;
   private int winW;
   private int winH;
   private int tabsX;
   private int tabsY;
   private int tabsW;
   private int tabsH;
   private int tabGap = 5;
   private int contentX;
   private int contentY;
   private int contentH;
   private int footerY;
   private int controlsX;
   private int controlsW;
   private int railX;
   private int railW;
   private boolean showRail;

   public LyricsConfigScreen(Screen parent, LyricsConfig config) {
      this(parent, config, null, null);
   }

   public LyricsConfigScreen(Screen parent, LyricsConfig config, LyricsController controller, NowPlayingHud hud) {
      super(Component.literal("Rikixz Lyric"));
      this.parent = parent;
      this.config = config;
      this.controller = controller;
      this.hud = hud;
   }

   private void layout() {
      int margin = Math.max(6, Math.min(10, this.width / 80));
      this.winW = Math.min(600, this.width - margin * 2);
      this.winH = Math.min(340, this.height - margin * 2);
      this.winX = (this.width - this.winW) / 2;
      this.winY = (this.height - this.winH) / 2;

      int headerH = this.winH < 250 ? 42 : 48;
      this.tabsX = this.winX + 10;
      this.tabsW = this.winW - 20;
      this.tabsH = 20;
      this.tabsY = this.winY + headerH + 4;

      this.contentX = this.winX + 10;
      this.contentY = this.tabsY + this.tabsH + 7;
      this.footerY = this.winY + this.winH - 26;
      this.contentH = Math.max(64, this.footerY - 7 - this.contentY);

      this.showRail = this.winW >= 560 && this.contentH >= 150;
      this.railW = this.showRail ? Math.min(190, this.winW / 3) : 0;
      this.railX = this.winX + this.winW - 10 - this.railW;
      this.controlsX = this.contentX;
      this.controlsW = this.showRail ? this.railX - 8 - this.controlsX : this.winW - 20;
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.layout();

      Tab[] tabs = Tab.values();
      int tabBtnW = (this.tabsW - (tabs.length - 1) * this.tabGap) / tabs.length;
      for (int i = 0; i < tabs.length; i++) {
         Tab tab = tabs[i];
         boolean selected = tab == this.activeTab;
         this.addRenderableWidget(new VisualButton(
            this.tabsX + i * (tabBtnW + this.tabGap), this.tabsY, tabBtnW, this.tabsH,
            Component.literal(tab.label),
            () -> {
               this.activeTab = tab;
               this.init();
            },
            selected ? VisualTheme.ACCENT_MINT : VisualTheme.ACCENT_LAVENDER,
            selected
         ));
      }

      int rowH = this.contentH >= 150 ? 26 : 22;
      int gap = this.contentH >= 120 ? 5 : 4;
      int colGap = 6;
      int colW = (this.controlsW - colGap) / 2;
      int col0 = this.controlsX;
      int col1 = this.controlsX + colW + colGap;
      int used;

      switch (this.activeTab) {
         case SKY -> {
            int y = this.contentY;
            this.addRenderableWidget(new VisualToggle(col0, y, colW, rowH, "Sky Mode",
               "Float the lyrics above your view", this.config.skyMode, v -> {
                  this.config.skyMode = v;
                  this.config.markDirty();
               }));
            this.addRenderableWidget(new VisualToggle(col1, y, colW, rowH, "Always In Front",
               "Follow your look direction", this.config.alwaysInFront, v -> {
                  this.config.alwaysInFront = v;
                  this.config.markDirty();
               }));

            y += rowH + gap;
            this.addRenderableWidget(new VisualSlider(col0, y, colW, rowH, "Text Size", 0.4, 3.0,
               this.config.textScale, v -> {
                  this.config.textScale = v.floatValue();
                  this.config.markDirty();
               }, v -> String.format("%.1f×", v)));
            this.addRenderableWidget(new VisualSlider(col1, y, colW, rowH, "Sky Height", 1.0, 15.0,
               this.config.skyHeight, v -> {
                  this.config.skyHeight = v;
                  this.config.markDirty();
               }, v -> String.format("+%.1fm", v)));

            y += rowH + gap;
            this.addRenderableWidget(new VisualSlider(col0, y, colW, rowH, "Sky Distance", 4.0, 25.0,
               this.config.skyDistance, v -> {
                  this.config.skyDistance = v;
                  this.config.markDirty();
               }, v -> String.format("%.1fm", v)));
            this.addRenderableWidget(new VisualSlider(col1, y, colW, rowH, "Sky Angle", 10.0, 50.0,
               this.config.skyAngle, v -> {
                  this.config.skyAngle = v;
                  this.config.markDirty();
               }, v -> String.format("%.0f°", v)));
            used = y + rowH - this.contentY;
         }
         case HUD -> {
            int y = this.contentY;
            this.addRenderableWidget(new VisualToggle(col0, y, colW, rowH, "Enable Song HUD",
               "Show the glass now-playing card", this.config.hudEnabled, v -> {
                  this.config.hudEnabled = v;
                  this.config.markDirty();
               }));
            this.addRenderableWidget(new VisualToggle(col1, y, colW, rowH, "Always Visible",
               "Keep the card while stopped", this.config.hudAlwaysShow, v -> {
                  this.config.hudAlwaysShow = v;
                  this.config.markDirty();
               }));

            y += rowH + gap;
            this.addRenderableWidget(new VisualSlider(this.controlsX, y, this.controlsW, rowH, "HUD Scale", 0.5, 2.0,
               this.config.hudScale, v -> {
                  this.config.hudScale = Math.round(v * 20.0) / 20.0;
                  this.config.markDirty();
               }, v -> String.format("%.0f%%", Math.round(v * 20.0) * 5.0)));

            y += rowH + gap;
            int dragH = Math.max(22, Math.min(24, rowH + 2));
            this.addRenderableWidget(new VisualButton(this.controlsX, y, this.controlsW, dragH,
               Component.literal("✦  Drag the card on screen  ✦"), () -> {
                  if (this.minecraft != null && this.minecraft.gui != null) {
                     this.minecraft.gui.setScreen(new HudPositionScreen(this, this.config, this.controller, this.hud));
                  }
               }, VisualTheme.ACCENT_MINT, true));

            y += dragH + gap;
            int presetGap = 5;
            int presetW = (this.controlsW - presetGap * 2) / 3;
            this.addRenderableWidget(new VisualButton(this.controlsX, y, presetW, 20, Component.literal("Center"), () -> {
               this.config.hudPercentX = 0.5;
               this.config.hudPercentY = 0.5;
               this.config.markDirty();
            }, VisualTheme.ACCENT_LAVENDER, false));
            this.addRenderableWidget(new VisualButton(this.controlsX + presetW + presetGap, y, presetW, 20,
               Component.literal("Top"), () -> {
                  this.config.hudPercentX = 0.5;
                  this.config.hudPercentY = 0.22;
                  this.config.markDirty();
               }, VisualTheme.ACCENT_LAVENDER, false));
            this.addRenderableWidget(new VisualButton(this.controlsX + (presetW + presetGap) * 2, y, presetW, 20,
               Component.literal("Bottom"), () -> {
                  this.config.hudPercentX = 0.5;
                  this.config.hudPercentY = 0.78;
                  this.config.markDirty();
               }, VisualTheme.ACCENT_LAVENDER, false));
            used = y + 20 - this.contentY;
         }
         case EFFECTS -> {
            int y = this.contentY;
            this.addRenderableWidget(new VisualToggle(col0, y, colW, rowH, "Cool Visuals",
               "Active word pop and bloom", this.config.coolVisuals, v -> {
                  this.config.coolVisuals = v;
                  this.config.markDirty();
               }));
            this.addRenderableWidget(new VisualToggle(col1, y, colW, rowH, "Floating Bobbing",
               "Gentle ambient motion", this.config.floatingMotion, v -> {
                  this.config.floatingMotion = v;
                  this.config.markDirty();
               }));

            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(col0, y, colW, rowH, "Glow Outline",
               "Soft luminous word edges", this.config.glowOutline, v -> {
                  this.config.glowOutline = v;
                  this.config.markDirty();
               }));
            this.addRenderableWidget(new VisualToggle(col1, y, colW, rowH, "Word Particles",
               "Notes and sparks on each word", this.config.particles, v -> {
                  this.config.particles = v;
                  this.config.markDirty();
               }));

            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(col0, y, colW, rowH, "Song Title Card",
               "Show a title when a track starts", this.config.showTitleCard, v -> {
                  this.config.showTitleCard = v;
                  this.config.markDirty();
               }));
            this.addRenderableWidget(new VisualButton(col1, y, colW, rowH,
               Component.literal("Style · " + this.config.colorStyle.label), () -> {
                  LyricsConfig.ColorStyle[] styles = LyricsConfig.ColorStyle.values();
                  this.config.colorStyle = styles[(this.config.colorStyle.ordinal() + 1) % styles.length];
                  this.config.markDirty();
                  this.init();
               }, VisualTheme.ACCENT_AMBER, false));
            used = y + rowH - this.contentY;
         }
         case SYNC -> {
            int y = this.contentY;
            this.addRenderableWidget(new VisualSlider(this.controlsX, y, this.controlsW, rowH, "Global Offset",
               -3000.0, 3000.0, this.config.globalOffsetMs, v -> {
                  this.config.globalOffsetMs = (int)(Math.round(v / 50.0) * 50L);
                  this.config.markDirty();
               }, v -> {
                  long ms = Math.round(v / 50.0) * 50L;
                  String suffix = Math.abs(ms) < 25L ? "" : (ms > 0L ? " sooner" : " later");
                  return String.format("%+.2fs%s", ms / 1000.0, suffix);
               }));

            y += rowH + gap;
            int nudgeGap = 4;
            int nudgeW = (this.controlsW - nudgeGap * 4) / 5;
            this.addRenderableWidget(new VisualButton(this.controlsX, y, nudgeW, 18, Component.literal("-0.5s"),
               () -> this.nudgeOffset(-500), VisualTheme.ACCENT_LAVENDER, false));
            this.addRenderableWidget(new VisualButton(this.controlsX + (nudgeW + nudgeGap), y, nudgeW, 18,
               Component.literal("-0.1s"), () -> this.nudgeOffset(-100), VisualTheme.ACCENT_LAVENDER, false));
            this.addRenderableWidget(new VisualButton(this.controlsX + (nudgeW + nudgeGap) * 2, y, nudgeW, 18,
               Component.literal("Reset"), () -> {
                  this.config.globalOffsetMs = 0;
                  this.config.markDirty();
                  this.init();
               }, VisualTheme.ACCENT_MINT, false));
            this.addRenderableWidget(new VisualButton(this.controlsX + (nudgeW + nudgeGap) * 3, y, nudgeW, 18,
               Component.literal("+0.1s"), () -> this.nudgeOffset(100), VisualTheme.ACCENT_LAVENDER, false));
            this.addRenderableWidget(new VisualButton(this.controlsX + (nudgeW + nudgeGap) * 4, y, nudgeW, 18,
               Component.literal("+0.5s"), () -> this.nudgeOffset(500), VisualTheme.ACCENT_LAVENDER, false));

            y += 18 + gap;
            this.addRenderableWidget(new VisualToggle(col0, y, colW, rowH, "Status Messages",
               "Search and found updates", this.config.statusMessages, v -> {
                  this.config.statusMessages = v;
                  this.config.markDirty();
               }));
            this.addRenderableWidget(new VisualToggle(col1, y, colW, rowH, "See Through Walls",
               "Draw lyrics through blocks", this.config.seeThroughWalls, v -> {
                  this.config.seeThroughWalls = v;
                  this.config.markDirty();
               }));
            used = y + rowH - this.contentY;
         }
         default -> used = 0;
      }

      int doneW = 74;
      this.addRenderableWidget(new VisualButton(this.winX + this.winW - doneW - 10, this.footerY,
         doneW, 20, Component.literal("Done"), this::onClose, VisualTheme.ACCENT_MINT, true));

      this.hintUsed = used;
   }

   private int hintUsed;

   private void nudgeOffset(int delta) {
      this.config.globalOffsetMs = (int)Math.max(-3000L, Math.min(3000L, this.config.globalOffsetMs + delta));
      this.config.markDirty();
      this.init();
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      this.layout();
      float time = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;

      VisualTheme.drawBackdrop(g, this.width, this.height, time);

      VisualTheme.fillRounded(g, this.winX - 3, this.winY - 3, this.winW + 6, this.winH + 6, 17, 0x1AC4B5FD);
      VisualTheme.drawRoundedPanel(g, this.winX, this.winY, this.winW, this.winH, 14, 0xF40B1020, 0x3AFFFFFF);
      g.fillGradient(this.winX + 14, this.winY + 1, this.winX + this.winW - 14, this.winY + 20, 0x14FFFFFF, 0x00FFFFFF);
      VisualTheme.drawHorizontalGradient(g, this.winX + 22, this.winY + 1,
         Math.max(4, this.winW - 44), 1, 0x99C4B5FD, 0x8899F6E4);

      this.drawHeader(g, time);

      // Segmented tab track behind the buttons
      VisualTheme.fillRounded(g, this.tabsX - 2, this.tabsY - 2, this.tabsW + 4, this.tabsH + 4, 9, 0x52000000);

      if (this.showRail) {
         this.drawPreviewRail(g, time);
      }

      this.drawHint(g);

      g.fill(this.winX + 10, this.footerY - 4, this.winX + this.winW - 10, this.footerY - 3, 0x14FFFFFF);
      int hintMax = this.winW - 110;
      String footerHint = "v1.0.0  ·  Changes save automatically  ·  ESC to close";
      g.text(this.font, VisualTheme.clip(this.font, footerHint, hintMax), this.winX + 12,
         this.footerY + 6, VisualTheme.TEXT_DARK);

      super.extractRenderState(g, mouseX, mouseY, partialTick);
   }

   private void drawHeader(GuiGraphicsExtractor g, float time) {
      int headerH = this.tabsY - 4 - this.winY;
      int mark = Math.min(28, headerH - 12);
      int markX = this.winX + 11;
      int markY = this.winY + (headerH - mark) / 2;
      VisualTheme.drawRoundedPanel(g, markX, markY, mark, mark, 7, 0x1AFFFFFF, 0x44C4B5FD);
      try {
         g.blit(LOGO_TEXTURE, markX + 1, markY + 1, markX + mark - 1, markY + mark - 1, 0.0F, 1.0F, 0.0F, 1.0F);
      } catch (Throwable ignored) {
         g.centeredText(this.font, "♪", markX + mark / 2, markY + mark / 2 - 4, VisualTheme.ACCENT_MINT);
      }

      int textX = markX + mark + 9;
      boolean wideHeader = this.winW >= 520;
      int titleY = wideHeader
         ? this.winY + (headerH >= 46 ? 8 : 7)
         : this.winY + (headerH - 9) / 2;
      float titleScale = 1.15F;
      g.pose().pushMatrix();
      g.pose().translate((float)textX, (float)titleY);
      g.pose().scale(titleScale, titleScale);
      g.text(this.font, Component.literal("Rikixz Lyric").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF);
      g.pose().popMatrix();

      if (wideHeader) {
         g.text(this.font, "Sound, written in the sky", textX, titleY + 12, VisualTheme.TEXT_MUTED);
      }

      MediaSample media = this.controller != null ? this.controller.currentMedia() : MediaSample.none();
      boolean hasTrack = media.hasTrack();
      boolean playing = hasTrack && (this.controller == null || this.controller.isPlaying());
      String app = hasTrack && media.app() != null && !media.app().isBlank() ? media.app().toUpperCase() : "";
      String status = hasTrack
         ? (playing ? "NOW PLAYING" : "PAUSED") + (app.isBlank() ? "" : "  ·  " + app)
         : "STANDBY";
      String title = hasTrack ? "  ·  " + KhmerTextUtil.shape(media.title()) : "";

      int pillMax = Math.min(230, this.winW / 2);
      int pillW = Math.min(pillMax, this.font.width(status + title) + 26);
      int pillX = this.winX + this.winW - pillW - 10;
      int pillH = 18;
      int pillY = this.winY + (headerH - pillH) / 2;
      String pillText = VisualTheme.clip(this.font, status + title, Math.max(20, pillW - 24));
      int statusColor = hasTrack ? (playing ? VisualTheme.ACCENT_MINT : VisualTheme.ACCENT_AMBER) : VisualTheme.TEXT_MUTED;

      VisualTheme.drawRoundedPanel(g, pillX, pillY, pillW, pillH, 8, 0x66000000, 0x2AFFFFFF);
      if (playing) {
         VisualTheme.drawEqualizer(g, pillX + 7, pillY + 5, 12, 8, time,
            VisualTheme.ACCENT_LAVENDER, VisualTheme.ACCENT_MINT, true);
      } else {
         VisualTheme.drawStatusDot(g, pillX + 8, pillY + 7, statusColor, false, time);
      }
      g.text(this.font, pillText, pillX + 22, pillY + 5, statusColor);
   }

   private void drawPreviewRail(GuiGraphicsExtractor g, float time) {
      int y = this.contentY - 4;
      int h = this.contentH + 8;
      VisualTheme.drawRoundedPanel(g, this.railX, y, this.railW, h, 12, 0xC00A0F1B, 0x33FFFFFF);
      VisualTheme.drawHorizontalGradient(g, this.railX + 18, y + 1,
         Math.max(4, this.railW - 36), 1, 0x99C4B5FD, 0x8899F6E4);
      VisualTheme.drawKicker(g, this.font, this.railX + 10, y + 10, "LIVE PREVIEW", VisualTheme.ACCENT_MINT);

      int areaX = this.railX + 9;
      int areaY = y + 26;
      int areaW = this.railW - 18;
      int areaH = h - 42;

      switch (this.activeTab) {
         case HUD -> this.drawHudPreview(g, areaX, areaY, areaW, areaH);
         case SKY, EFFECTS -> this.drawSkyPreview(g, areaX, areaY, areaW, areaH, time);
         case SYNC -> this.drawSyncPreview(g, areaX, areaY, areaW, areaH);
      }

      g.centeredText(this.font, "Live from your player", this.railX + this.railW / 2, y + h - 13,
         VisualTheme.TEXT_DARK);
   }

   private void drawHudPreview(GuiGraphicsExtractor g, int x, int y, int w, int h) {
      float scale = Math.min((w - 4) / (float)NowPlayingHud.WIDTH, 0.62F);
      scale = Math.max(0.28F, Math.min(scale, (h - 18) / (float)NowPlayingHud.HEIGHT));
      int cardW = (int)(NowPlayingHud.WIDTH * scale);
      int cardH = (int)(NowPlayingHud.HEIGHT * scale);
      int cardX = x + (w - cardW) / 2;
      int cardY = y + Math.max(2, (h - cardH) / 2 - 2);

      if (this.hud != null) {
         this.hud.renderHudCard(g, cardX, cardY, scale, false, true);
      } else {
         VisualTheme.drawRoundedPanel(g, cardX, cardY, Math.max(40, cardW), Math.max(24, cardH), 10,
            0xE60B1020, 0x33FFFFFF);
         g.centeredText(this.font, "HUD preview", cardX + cardW / 2, cardY + cardH / 2 - 4,
            VisualTheme.TEXT_MUTED);
      }
   }

   private void drawSkyPreview(GuiGraphicsExtractor g, int x, int y, int w, int h, float time) {
      VisualTheme.drawRoundedPanel(g, x, y, w, h, 10, 0x66070A12, 0x22FFFFFF);
      VisualTheme.drawGlow(g, x + w / 2, y + h - 6, Math.max(30, w / 2), VisualTheme.ACCENT_VIOLET);

      for (int i = 0; i < 16; i++) {
         int sx = x + 6 + (i * 53 + 11) % Math.max(8, w - 12);
         int sy = y + 8 + (i * 37 + 5) % Math.max(8, h - 26);
         float twinkle = 0.22F + 0.18F * (float)Math.sin(time * 1.4F + i);
         g.fill(sx, sy, sx + 1, sy + 1, VisualTheme.alpha(0xFFFFFF, (int)(twinkle * 255.0F)));
      }

      int cx = x + w / 2;
      int cy = y + h / 2;
      float bounce = this.config.floatingMotion ? (float)Math.sin(time * 2.6F) * 2.0F : 0.0F;
      int color = this.config.colorStyle.hue < 0.0F
         ? Anim.hsv(time * 0.16F, 0.72F, 1.0F)
         : Anim.hsv(this.config.colorStyle.hue, this.config.colorStyle.saturation, 1.0F);

      String line1 = "We're running through";
      String line2 = "the neon night";
      String line3 = "Rikixz · Neon Skyline";

      g.centeredText(this.font, VisualTheme.clip(this.font, line1, w - 12), cx, cy - 16 + (int)bounce,
         VisualTheme.TEXT_MUTED);
      g.centeredText(this.font, VisualTheme.clip(this.font, line2, w - 12), cx + 1, cy - 3 + (int)bounce,
         VisualTheme.alpha(color, 0x28));
      g.centeredText(this.font, VisualTheme.clip(this.font, line2, w - 12), cx, cy - 4 + (int)bounce, color);
      g.centeredText(this.font, VisualTheme.clip(this.font, line3, w - 12), cx, cy + 10 + (int)bounce,
         VisualTheme.TEXT_DARK);

      if (this.activeTab == Tab.EFFECTS) {
         g.text(this.font, "♪", x + 8, y + h - 20, VisualTheme.ACCENT_MINT);
         g.text(this.font, "♫", x + w - 14, y + 16, VisualTheme.ACCENT_LAVENDER);
      } else {
         g.centeredText(this.font, "☁", cx, y + 8, VisualTheme.alpha(VisualTheme.ACCENT_LAVENDER, 0x66));
      }
   }

   private void drawSyncPreview(GuiGraphicsExtractor g, int x, int y, int w, int h) {
      VisualTheme.drawRoundedPanel(g, x, y, w, h, 10, 0x66070A12, 0x22FFFFFF);
      int cx = x + w / 2;

      g.centeredText(this.font, "GLOBAL OFFSET", cx, y + 14, VisualTheme.TEXT_DARK);
      double seconds = this.config.globalOffsetMs / 1000.0;
      String value = String.format("%+.2fs", seconds);
      float scale = 1.8F;
      int valueY = y + 30;
      g.pose().pushMatrix();
      g.pose().translate((float)cx, (float)valueY);
      g.pose().scale(scale, scale);
      g.centeredText(this.font, value, 0, 0, VisualTheme.ACCENT_LAVENDER);
      g.pose().popMatrix();

      String sync = Math.abs(this.config.globalOffsetMs) < 25 ? "Perfectly in sync"
         : (this.config.globalOffsetMs > 0 ? "Lyrics arrive sooner" : "Lyrics arrive later");
      g.centeredText(this.font, sync, cx, y + 51, VisualTheme.ACCENT_MINT);
      g.fill(x + 14, y + 62, x + w - 14, y + 63, 0x22FFFFFF);

      int lineY = y + 72;
      g.text(this.font, "[ / ]  nudge sync", x + 12, lineY, VisualTheme.TEXT_MUTED);
      g.text(this.font, "[ K ]  toggle lyrics", x + 12, lineY + 12, VisualTheme.TEXT_MUTED);
      g.text(this.font, "[ H ]  HUD placement", x + 12, lineY + 24, VisualTheme.TEXT_MUTED);
      g.text(this.font, "[ \\ ]  reset timer", x + 12, lineY + 36, VisualTheme.TEXT_MUTED);
   }

   private void drawHint(GuiGraphicsExtractor g) {
      int remaining = this.contentH - this.hintUsed;
      if (remaining < 24) {
         return;
      }

      int hintY = this.contentY + this.hintUsed + 7;
      int hintH = Math.min(22, remaining - 7);
      VisualTheme.drawRoundedPanel(g, this.controlsX, hintY, this.controlsW, hintH, 7, 0x99101522, 0x1EFFFFFF);
      g.fill(this.controlsX + 8, hintY + hintH / 2 - 1, this.controlsX + 11, hintY + hintH / 2 + 2,
         VisualTheme.ACCENT_MINT);

      String hint = switch (this.activeTab) {
         case SKY -> "Sky mode floats lyrics above your view. Song HUD placement is separate.";
         case HUD -> String.format("Card at %.0f%% / %.0f%% · drag mode opens the full-size editor",
            this.config.hudPercentX * 100.0, this.config.hudPercentY * 100.0);
         case EFFECTS -> "Active style: " + this.config.colorStyle.label + " · effects apply word by word.";
         case SYNC -> "In game: [ / ] nudge sync · [K] toggle lyrics · [H] HUD editor.";
      };
      g.text(this.font, VisualTheme.clip(this.font, hint, this.controlsW - 22),
         this.controlsX + 16, hintY + (hintH - 8) / 2, VisualTheme.TEXT_MUTED);
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
