package com.realtimelyrics;

import com.realtimelyrics.hud.HudPositionScreen;
import com.realtimelyrics.hud.NowPlayingHud;
import com.realtimelyrics.media.MediaSource;
import com.realtimelyrics.render.LyricsRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class RealTimeLyricsClient implements ClientModInitializer {
   public static final String MOD_ID = "rikixz_lyric";
   public static final Logger LOG = LoggerFactory.getLogger("RikixzLyric");
   private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rikixz_lyric", "lyrics"));
   private LyricsConfig config;
   private MediaSource source;
   private LyricsController controller;
   private NowPlayingHud hud;
   private KeyMapping toggleKey;
   private KeyMapping earlierKey;
   private KeyMapping laterKey;
   private KeyMapping restartKey;
   private KeyMapping settingsKey;
   private KeyMapping hudKey;

   @Override
   public void onInitializeClient() {
      this.config = LyricsConfig.load();
      this.source = MediaSource.forThisOs();
      LyricsRenderer renderer = new LyricsRenderer(this.config);
      this.controller = new LyricsController(this.config, this.source, renderer);
      this.hud = new NowPlayingHud(this.config, this.controller);

      this.toggleKey = register("toggle", GLFW.GLFW_KEY_K);
      this.earlierKey = register("earlier", GLFW.GLFW_KEY_RIGHT_BRACKET);
      this.laterKey = register("later", GLFW.GLFW_KEY_LEFT_BRACKET);
      this.restartKey = register("restart", GLFW.GLFW_KEY_BACKSLASH);
      this.settingsKey = register("settings", GLFW.GLFW_KEY_UNKNOWN);
      this.hudKey = register("hud_position", GLFW.GLFW_KEY_H);

      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("rikixz_lyric", "now_playing_hud"), this.hud);

      ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
         if (screen instanceof PauseScreen pause && pause.showsPauseMenu()) {
            Screens.getWidgets(screen).add(
               Button.builder(Component.literal("♫ Lyrics"), b -> {
                  if (mc.gui != null) {
                     mc.gui.setScreen(new LyricsConfigScreen(screen, this.config, this.controller, this.hud));
                  }
               })
                  .bounds(8, 8, 80, 20)
                  .tooltip(Tooltip.create(Component.literal("Rikixz Lyric settings")))
                  .build()
            );
         }
      });

      ClientLifecycleEvents.CLIENT_STARTED.register(mc -> this.source.start());
      ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
         this.source.stop();
         this.config.save();
      });
      Runtime.getRuntime().addShutdownHook(new Thread(this.source::stop, "RikixzLyric-Shutdown"));
      ClientTickEvents.END_CLIENT_TICK.register(mc -> {
         this.handleKeys(mc);
         this.controller.tick(mc);
         this.config.saveIfDirty();
      });
      LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> renderer.render(ctx, this.controller));
      LOG.info("Rikixz Lyric ready with Song HUD and Front-view lyrics");
   }

   private static KeyMapping register(String name, int key) {
      return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.rikixz_lyric." + name, InputConstants.Type.KEYSYM, key, CATEGORY));
   }

   private void handleKeys(Minecraft mc) {
      while (this.toggleKey.consumeClick()) {
         this.config.enabled = !this.config.enabled;
         this.config.save();
         if (mc.player != null) {
            mc.player.sendOverlayMessage(
               Component.literal(this.config.enabled ? "♫ Lyrics on" : "Lyrics off")
                  .withStyle(this.config.enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY)
            );
         }
      }

      while (this.earlierKey.consumeClick()) {
         this.controller.nudge(mc, 200L);
      }

      while (this.laterKey.consumeClick()) {
         this.controller.nudge(mc, -200L);
      }

      while (this.restartKey.consumeClick()) {
         this.controller.restartTiming(mc);
      }

      while (this.settingsKey.consumeClick()) {
         if (mc.gui != null) {
            mc.gui.setScreen(new LyricsConfigScreen(mc.gui.screen(), this.config, this.controller, this.hud));
         }
      }

      while (this.hudKey.consumeClick()) {
         if (mc.gui != null) {
            mc.gui.setScreen(new HudPositionScreen(mc.gui.screen(), this.config, this.controller, this.hud));
         }
      }
   }
}
