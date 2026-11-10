package com.realtimelyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class LyricsConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public boolean enabled = true;
   public double distance = 4.5;
   public double textScale = 1.0;
   public boolean particles = true;
   public boolean showTitleCard = true;
   public boolean seeThroughWalls = false;
   public boolean glowOutline = true;
   public boolean statusMessages = true;
   public int globalOffsetMs = 0;
   public boolean followPlayer = true;
   public boolean alwaysInFront = true;
   public boolean skyMode = true; // "lyric on sky pls not on me"
   public double skyAngle = 26.0; // Elevation angle into the sky (degrees)
   public double skyHeight = 5.0; // Vertical height above eye level (blocks)
   public double skyDistance = 10.5; // Distance in the sky (blocks)
   public boolean floatingMotion = true;
   public boolean coolVisuals = true;
   public double spreadDegrees = 20.0;
   public double heightOffset = 0.0;
   public LyricsConfig.ColorStyle colorStyle = LyricsConfig.ColorStyle.RAINBOW;

   // Song HUD Overlay settings
   public boolean hudEnabled = true;
   public double hudPercentX = 0.5; // Default middle of screen
   public double hudPercentY = 0.5; // Default middle of screen
   public double hudScale = 1.0;
   public boolean hudAlwaysShow = false;

   private transient volatile boolean dirty;

   private static Path path() {
      return FabricLoader.getInstance().getConfigDir().resolve("realtimelyrics.json");
   }

   public static LyricsConfig load() {
      Path p = path();
      if (Files.exists(p)) {
         try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            LyricsConfig c = (LyricsConfig)GSON.fromJson(r, LyricsConfig.class);
            if (c != null) {
               c.sanitize();
               c.save();
               return c;
            }
         } catch (Exception e) {
            Path backup = p.resolveSibling("realtimelyrics.json.broken");
            RealTimeLyricsClient.LOG.warn("Could not read {}, using defaults (old file kept as {})", new Object[]{p, backup.getFileName(), e});

            try {
               Files.move(p, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception var4) {
            }
         }
      }

      LyricsConfig c = new LyricsConfig();
      c.save();
      return c;
   }

   private void sanitize() {
      if (this.colorStyle == null) {
         this.colorStyle = LyricsConfig.ColorStyle.RAINBOW;
      }

      this.distance = clamp(this.distance, 2.0, 12.0, 4.5);
      this.textScale = clamp(this.textScale, 0.4, 3.0, 1.0);
      this.spreadDegrees = clamp(this.spreadDegrees, 0.0, 40.0, 20.0);
      this.heightOffset = clamp(this.heightOffset, -1.5, 2.5, 0.0);
      this.globalOffsetMs = (int)clamp(this.globalOffsetMs, -3000.0, 3000.0, 0.0);
      this.skyAngle = clamp(this.skyAngle, 10.0, 50.0, 26.0);
      this.skyHeight = clamp(this.skyHeight, 1.0, 15.0, 5.0);
      this.skyDistance = clamp(this.skyDistance, 4.0, 25.0, 10.5);
      this.hudPercentX = clamp(this.hudPercentX, 0.02, 0.98, 0.5);
      this.hudPercentY = clamp(this.hudPercentY, 0.02, 0.98, 0.5);
      this.hudScale = clamp(this.hudScale, 0.5, 2.5, 1.0);
   }

   private static double clamp(double v, double min, double max, double fallback) {
      return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : fallback;
   }

   public void markDirty() {
      this.dirty = true;
   }

   public void saveIfDirty() {
      if (this.dirty) {
         this.save();
      }
   }

   public synchronized void save() {
      this.dirty = false;

      try {
         Path p = path();
         Files.createDirectories(p.getParent());
         Path tmp = p.resolveSibling("realtimelyrics.json.tmp");

         try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
         }

         try {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (Exception e) {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception e) {
         RealTimeLyricsClient.LOG.warn("Could not save config", e);
      }
   }

   @Environment(EnvType.CLIENT)
   public enum ColorStyle {
      RAINBOW("Rainbow", -1.0F, 0.3F),
      WHITE("White", 0.66F, 0.0F),
      GOLD("Gold", 0.12F, 0.45F),
      AQUA("Aqua", 0.5F, 0.4F),
      PINK("Pink", 0.92F, 0.38F),
      PURPLE("Purple", 0.76F, 0.38F),
      GREEN("Green", 0.33F, 0.38F),
      CYBERPUNK("Cyberpunk", 0.85F, 0.85F),
      SUNSET("Sunset", 0.06F, 0.85F),
      MINT("Mint", 0.44F, 0.65F);

      public final String label;
      public final float hue;
      public final float saturation;

      ColorStyle(String label, float hue, float saturation) {
         this.label = label;
         this.hue = hue;
         this.saturation = saturation;
      }
   }
}
