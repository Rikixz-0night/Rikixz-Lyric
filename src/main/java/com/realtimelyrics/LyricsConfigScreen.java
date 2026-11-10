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

@Environment(EnvType.CLIENT)
public final class LyricsConfigScreen extends Screen {
   public enum Tab {
      SKY("☁ Sky Lyrics"),
      HUD("♫ Song HUD"),
      EFFECTS("✨ Visual FX"),
      SYNC("⚙ Audio & Sync");

      final String label;
      Tab(String label) { this.label = label; }
   }

   private final Screen parent;
   private final LyricsConfig config;
   private final LyricsController controller;
   private final NowPlayingHud hud;
   private Tab activeTab = Tab.SKY;

   public LyricsConfigScreen(Screen parent, LyricsConfig config) {
      this(parent, config, null, null);
   }

   public LyricsConfigScreen(Screen parent, LyricsConfig config, LyricsController controller, NowPlayingHud hud) {
      super(Component.literal("♫ Rikixz Lyric"));
      this.parent = parent;
      this.config = config;
      this.controller = controller;
      this.hud = hud;
   }

   @Override
   protected void init() {
      this.clearWidgets();

      int winW = Math.min(480, this.width - 16);
      int winH = Math.min(290, this.height - 16);
      int winX = (this.width - winW) / 2;
      int winY = (this.height - winH) / 2;

      // Tab navigation buttons
      Tab[] tabs = Tab.values();
      int tabGap = 4;
      int tabW = (winW - 16 - (tabs.length - 1) * tabGap) / tabs.length;
      int tabY = winY + 42;

      for (int i = 0; i < tabs.length; i++) {
         Tab t = tabs[i];
         int tx = winX + 8 + i * (tabW + tabGap);
         boolean isSelected = (t == this.activeTab);

         VisualButton tabBtn = new VisualButton(
            tx, tabY, tabW, 20,
            Component.literal(t.label),
            () -> {
               this.activeTab = t;
               this.init();
            },
            isSelected ? VisualTheme.ACCENT_CYAN : VisualTheme.CARD_BORDER,
            isSelected
         );
         this.addRenderableWidget(tabBtn);
      }

      // Content area
      int contentX = winX + 8;
      int contentY = tabY + 26;
      int contentW = winW - 16;
      int leftW = (winW >= 420) ? (contentW - 170) : contentW;

      int rowH = 22;
      int gap = 3;

      switch (this.activeTab) {
         case SKY -> {
            int y = contentY;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Sky Mode", "Elevates lyrics 26° into sky, clearing character", this.config.skyMode, v -> {
               this.config.skyMode = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Always In Front", "Lyrics smoothly track your look direction", this.config.alwaysInFront, v -> {
               this.config.alwaysInFront = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualSlider(contentX, y, leftW, rowH, "Text Size", 0.4, 3.0, this.config.textScale, v -> {
               this.config.textScale = v.floatValue();
               this.config.markDirty();
            }, v -> String.format("%.0f%%", v * 100.0)));
            y += rowH + gap;
            this.addRenderableWidget(new VisualSlider(contentX, y, leftW, rowH, "Sky Height", 1.0, 15.0, this.config.skyHeight, v -> {
               this.config.skyHeight = v;
               this.config.markDirty();
            }, v -> String.format("+%.1fm up", v)));
            y += rowH + gap;
            this.addRenderableWidget(new VisualSlider(contentX, y, leftW, rowH, "Sky Distance", 4.0, 25.0, this.config.skyDistance, v -> {
               this.config.skyDistance = v;
               this.config.markDirty();
            }, v -> String.format("%.1fm out", v)));
            y += rowH + gap;
            this.addRenderableWidget(new VisualSlider(contentX, y, leftW, rowH, "Sky Angle", 10.0, 50.0, this.config.skyAngle, v -> {
               this.config.skyAngle = v;
               this.config.markDirty();
            }, v -> String.format("%.0f° pitch", v)));
         }
         case HUD -> {
            int y = contentY;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Enable Song HUD", "Show Now Playing glassmorphic music overlay", this.config.hudEnabled, v -> {
               this.config.hudEnabled = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Always Visible", "Keep HUD visible even when stopped/paused", this.config.hudAlwaysShow, v -> {
               this.config.hudAlwaysShow = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualSlider(contentX, y, leftW, rowH, "HUD Scale", 0.5, 2.0, this.config.hudScale, v -> {
               this.config.hudScale = Math.round(v * 20.0) / 20.0;
               this.config.markDirty();
            }, v -> String.format("%.0f%%", v * 100.0)));
            y += rowH + gap + 4;

            // Prominent Drag Editor Button
            this.addRenderableWidget(new VisualButton(contentX, y, leftW, 24, Component.literal("✦ DRAG HUD ON SCREEN ✦"), () -> {
               if (this.minecraft != null && this.minecraft.gui != null) {
                  this.minecraft.gui.setScreen(new HudPositionScreen(this, this.config, this.controller, this.hud));
               }
            }, VisualTheme.ACCENT_CYAN, true));

            y += 24 + gap + 2;
            int halfW = (leftW - gap) / 3;
            this.addRenderableWidget(new VisualButton(contentX, y, halfW, 18, Component.literal("⌖ Center"), () -> {
               this.config.hudPercentX = 0.5;
               this.config.hudPercentY = 0.5;
               this.config.markDirty();
            }));
            this.addRenderableWidget(new VisualButton(contentX + halfW + gap, y, halfW, 18, Component.literal("⬆ Top"), () -> {
               this.config.hudPercentX = 0.5;
               this.config.hudPercentY = 0.08;
               this.config.markDirty();
            }));
            this.addRenderableWidget(new VisualButton(contentX + (halfW + gap) * 2, y, halfW, 18, Component.literal("⬇ Bottom"), () -> {
               this.config.hudPercentX = 0.5;
               this.config.hudPercentY = 0.88;
               this.config.markDirty();
            }));
         }
         case EFFECTS -> {
            int y = contentY;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Cool Visuals", "Active word pop, glowing bloom & floating wave", this.config.coolVisuals, v -> {
               this.config.coolVisuals = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Floating Bobbing", "Smooth ambient floating motion", this.config.floatingMotion, v -> {
               this.config.floatingMotion = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Glow Outline", "Colored luminous outline around words", this.config.glowOutline, v -> {
               this.config.glowOutline = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Word Particles", "Pop musical notes & fireworks when words appear", this.config.particles, v -> {
               this.config.particles = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Song Title Card", "Large floating title card when song starts", this.config.showTitleCard, v -> {
               this.config.showTitleCard = v;
               this.config.markDirty();
            }));
            y += rowH + gap;

            // Color Style Cycler Button
            String colorLabel = "🎨 Style: " + this.config.colorStyle.label;
            this.addRenderableWidget(new VisualButton(contentX, y, leftW, 20, Component.literal(colorLabel), () -> {
               LyricsConfig.ColorStyle[] styles = LyricsConfig.ColorStyle.values();
               int next = (this.config.colorStyle.ordinal() + 1) % styles.length;
               this.config.colorStyle = styles[next];
               this.config.markDirty();
               this.init();
            }, VisualTheme.ACCENT_GOLD, false));
         }
         case SYNC -> {
            int y = contentY;
            this.addRenderableWidget(new VisualSlider(contentX, y, leftW, rowH, "Global Offset", -3000.0, 3000.0, this.config.globalOffsetMs, v -> {
               this.config.globalOffsetMs = (int)(Math.round(v / 50.0) * 50L);
               this.config.markDirty();
            }, v -> String.format("%+.2fs %s", Math.round(v / 50.0) * 50L / 1000.0, Math.abs(v) < 25.0 ? "" : (v > 0.0 ? "sooner" : "later"))));
            y += rowH + gap;

            // Quick timing nudge pills
            int btnW = (leftW - gap * 4) / 5;
            this.addRenderableWidget(new VisualButton(contentX, y, btnW, 18, Component.literal("-0.5s"), () -> {
               this.config.globalOffsetMs -= 500;
               this.config.markDirty();
               this.init();
            }));
            this.addRenderableWidget(new VisualButton(contentX + (btnW + gap), y, btnW, 18, Component.literal("-0.1s"), () -> {
               this.config.globalOffsetMs -= 100;
               this.config.markDirty();
               this.init();
            }));
            this.addRenderableWidget(new VisualButton(contentX + (btnW + gap) * 2, y, btnW, 18, Component.literal("Reset"), () -> {
               this.config.globalOffsetMs = 0;
               this.config.markDirty();
               this.init();
            }, VisualTheme.ACCENT_CYAN, false));
            this.addRenderableWidget(new VisualButton(contentX + (btnW + gap) * 3, y, btnW, 18, Component.literal("+0.1s"), () -> {
               this.config.globalOffsetMs += 100;
               this.config.markDirty();
               this.init();
            }));
            this.addRenderableWidget(new VisualButton(contentX + (btnW + gap) * 4, y, btnW, 18, Component.literal("+0.5s"), () -> {
               this.config.globalOffsetMs += 500;
               this.config.markDirty();
               this.init();
            }));
            y += 18 + gap + 3;

            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "Status Messages", "Show searching/found status above hotbar", this.config.statusMessages, v -> {
               this.config.statusMessages = v;
               this.config.markDirty();
            }));
            y += rowH + gap;
            this.addRenderableWidget(new VisualToggle(contentX, y, leftW, rowH, "See Through Walls", "Draw lyrics through blocks", this.config.seeThroughWalls, v -> {
               this.config.seeThroughWalls = v;
               this.config.markDirty();
            }));
         }
      }

      // Close & Save Button at bottom
      int footerY = winY + winH - 28;
      int doneW = 100;
      int doneX = winX + winW - doneW - 8;
      this.addRenderableWidget(new VisualButton(doneX, footerY, doneW, 20, Component.literal("✓ Done"), this::onClose, VisualTheme.ACCENT_CYAN, true));
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      // Dark translucent backdrop overlay
      g.fill(0, 0, this.width, this.height, VisualTheme.BG_OVERLAY);

      int winW = Math.min(480, this.width - 16);
      int winH = Math.min(290, this.height - 16);
      int winX = (this.width - winW) / 2;
      int winY = (this.height - winH) / 2;

      // Main Modal Panel with animated gradient border
      VisualTheme.drawPanel(g, winX, winY, winW, winH, VisualTheme.MODAL_BG, VisualTheme.MODAL_BORDER);
      VisualTheme.drawGradientBorder(g, winX, winY, winW, winH, VisualTheme.ACCENT_CYAN, VisualTheme.ACCENT_PURPLE);

      // Top Header Area
      g.fill(winX + 1, winY + 1, winX + winW - 1, winY + 38, VisualTheme.HEADER_BG);
      g.horizontalLine(winX + 1, winX + winW - 2, winY + 38, VisualTheme.CARD_BORDER);

      // Mod Title & Branding
      g.text(this.font, Component.literal("♫ RIKIXZ LYRIC").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), winX + 10, winY + 8, -1);
      g.text(this.font, Component.literal("v1.0.0 · by Rikixz").withStyle(ChatFormatting.DARK_GRAY), winX + 115, winY + 9, VisualTheme.TEXT_MUTED);

      // Live Now Playing Status in the Header!
      MediaSample media = (this.controller != null) ? this.controller.currentMedia() : MediaSample.none();
      boolean isPlaying = media.hasTrack() && (this.controller == null || this.controller.isPlaying());
      float time = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;

      int statusX = winX + 10;
      int statusY = winY + 23;

      if (isPlaying) {
         VisualTheme.drawEqualizer(g, statusX, statusY, 14, 9, time, VisualTheme.ACCENT_GREEN);
         String trackInfo = " " + media.title() + " - " + media.artist();
         if (this.font.width(trackInfo) > winW - 40) {
            trackInfo = this.font.plainSubstrByWidth(trackInfo, winW - 55) + "...";
         }
         g.text(this.font, KhmerTextUtil.shape(trackInfo), statusX + 16, statusY, VisualTheme.ACCENT_CYAN);
      } else {
         g.text(this.font, "⏹ No music playing · (Start Spotify, YouTube or Browser)", statusX, statusY, VisualTheme.TEXT_DARK);
      }

      // Live Preview Box on the Right Side (if window is wide enough)
      if (winW >= 420) {
         int contentY = winY + 68;
         int prevX = winX + winW - 156;
         int prevY = contentY;
         int prevW = 148;
         int prevH = winH - 100;

         VisualTheme.drawPanel(g, prevX, prevY, prevW, prevH, 0xD00A101D, VisualTheme.CARD_BORDER);
         g.centeredText(this.font, Component.literal("✦ LIVE PREVIEW ✦").withStyle(ChatFormatting.BOLD), prevX + prevW / 2, prevY + 6, VisualTheme.ACCENT_GOLD);

         switch (this.activeTab) {
            case SKY, EFFECTS -> {
               // Render preview of the sky lyrics
               int cx = prevX + prevW / 2;
               int cy = prevY + prevH / 2 - 8;
               float bounce = this.config.floatingMotion ? (float)Math.sin(time * 3.0F) * 4.0F : 0.0F;

               int textColor = (this.config.colorStyle.hue < 0.0F)
                  ? Anim.hsv(time * 0.2F, 0.85F, 1.0F)
                  : Anim.hsv(this.config.colorStyle.hue, this.config.colorStyle.saturation, 1.0F);

               g.centeredText(this.font, "♪ ♫ ♬ ♫ ♪", cx, cy - 14 + (int)bounce, VisualTheme.ACCENT_CYAN);
               g.centeredText(this.font, KhmerTextUtil.shape("ស្រឡាញ់អូន"), cx, cy + (int)bounce, textColor);
               g.centeredText(this.font, "Girl In My Dream", cx, cy + 14 + (int)bounce, textColor);

               g.centeredText(this.font, Component.literal("Sky View Demo").withStyle(ChatFormatting.DARK_GRAY), cx, prevY + prevH - 12, VisualTheme.TEXT_DARK);
            }
            case HUD -> {
               // Render miniature preview of the real HUD card
               int cardW = 136;
               float prevScale = (float)cardW / (float)NowPlayingHud.WIDTH;
               int cardX = prevX + (prevW - cardW) / 2;
               int cardY = prevY + (prevH - (int)(NowPlayingHud.HEIGHT * prevScale)) / 2 - 4;
               if (this.hud != null) {
                  this.hud.renderHudCard(g, cardX, cardY, prevScale, false, true);
               }
               g.centeredText(this.font, Component.literal("Now Playing Card").withStyle(ChatFormatting.DARK_GRAY), prevX + prevW / 2, prevY + prevH - 12, VisualTheme.TEXT_DARK);
            }
            case SYNC -> {
               int cx = prevX + prevW / 2;
               g.centeredText(this.font, "Shortcuts", cx, prevY + 20, VisualTheme.TEXT_PRIMARY);
               g.text(this.font, "[K] Toggle Lyrics", prevX + 10, prevY + 36, VisualTheme.TEXT_MUTED);
               g.text(this.font, "[H] Drag HUD Menu", prevX + 10, prevY + 48, VisualTheme.TEXT_MUTED);
               g.text(this.font, "[[ / ]] Nudge Offset", prevX + 10, prevY + 60, VisualTheme.TEXT_MUTED);
               g.text(this.font, "[\\] Reset Timer", prevX + 10, prevY + 72, VisualTheme.TEXT_MUTED);
            }
         }
      }

      // Footer divider & Hints
      int footerY = winY + winH - 28;
      g.horizontalLine(winX + 1, winX + winW - 2, footerY - 4, VisualTheme.CARD_BORDER);
      g.text(this.font, "✦ Press H to drag HUD · K to toggle", winX + 10, footerY + 6, VisualTheme.TEXT_DARK);

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
