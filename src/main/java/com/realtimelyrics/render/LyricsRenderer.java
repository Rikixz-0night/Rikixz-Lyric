package com.realtimelyrics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.realtimelyrics.LyricsConfig;
import com.realtimelyrics.LyricsController;
import com.realtimelyrics.lyrics.Lyrics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class LyricsRenderer {
   private static final float BLOCKS_PER_PX = 0.028F;
   private static final float MAX_ROW_PX = 190.0F;
   private final LyricsConfig config;
   private final List<FloatingText> texts = new ArrayList<>();
   private final Random random = new Random();
   private final long startNanos = System.nanoTime();
   private FloatingText currentLine;
   private FloatingText notes;
   private Lyrics shownLyrics;
   private int shownIndex = Integer.MIN_VALUE;
   private int lineCounter;
   private float trackHue = this.random.nextFloat();
   private String[] pendingTitle;
   private Vec3 anchor;
   private Vec3 anchorDelta = Vec3.ZERO;
   private Vec3 viewOrigin = Vec3.ZERO;
   private float viewYaw;
   private float viewPitch;
   private float viewHalfWidthDeg = 50.0F;
   private float viewHalfHeightDeg = 35.0F;
   private long lastFrameNanos;

   public LyricsRenderer(LyricsConfig config) {
      this.config = config;
   }

   public void clear() {
      long now = System.nanoTime();

      for (FloatingText t : this.texts) {
         this.retire(t, now);
      }

      this.currentLine = null;
      this.notes = null;
      this.shownLyrics = null;
      this.shownIndex = Integer.MIN_VALUE;
      this.pendingTitle = null;
   }

   public void onNewTrack(String title, String artist) {
      this.clear();
      this.trackHue = ((title + artist).hashCode() & 65535) / 65535.0F;
      this.lineCounter = 0;
      if (this.config.showTitleCard && !title.isBlank()) {
         this.pendingTitle = new String[]{title, artist};
      }
   }

   public void render(LevelRenderContext ctx, LyricsController controller) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (!this.config.enabled) {
            this.texts.clear();
            this.currentLine = null;
            this.notes = null;
            this.shownLyrics = null;
            this.shownIndex = Integer.MIN_VALUE;
            this.pendingTitle = null;
         } else {
            long now = System.nanoTime();
            float dt = this.lastFrameNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
            this.lastFrameNanos = now;
            float time = (float)(now - this.startNanos) / 1.0E9F;
            float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            Vec3 eye = mc.player.getEyePosition(partial);
            Vec3 oldAnchor = this.anchor;
            if (this.anchor != null && !(this.anchor.distanceToSqr(eye) > 256.0)) {
               this.anchor = this.anchor.lerp(eye, 1.0 - Math.exp(-dt * 6.0F));
            } else {
               this.anchor = eye;
            }

            this.anchorDelta = oldAnchor != null && !(oldAnchor.distanceToSqr(this.anchor) > 256.0) ? this.anchor.subtract(oldAnchor) : Vec3.ZERO;
            if (this.notes != null
               && this.notes.worldPos != null
               && !this.config.followPlayer
               && this.notes.worldPos.distanceToSqr(eye) > Math.pow(this.config.distance + 8.0, 2.0)) {
               this.retire(this.notes, now);
               this.notes = null;
            }

            CameraRenderState cam = ctx.levelState().cameraRenderState;
            this.viewOrigin = cam.pos;
            this.viewYaw = cam.yRot;
            float viewYaw = this.viewYaw;
            this.viewPitch = Math.max(-60.0F, Math.min(60.0F, cam.xRot));
            double vHalf = Math.toRadians(mc.options.fov().get().intValue() / 2.0);
            double aspect = mc.getWindow().getHeight() > 0
               ? (double)mc.getWindow().getWidth() / mc.getWindow().getHeight()
               : 1.7777777777777777;
            this.viewHalfWidthDeg = (float)Math.toDegrees(Math.atan(Math.tan(vHalf) * aspect));
            this.viewHalfHeightDeg = (float)Math.toDegrees(vHalf);
            Font font = mc.font;
            if (this.pendingTitle != null) {
               this.texts.add(this.spawnTitle(font, this.pendingTitle[0], this.pendingTitle[1], viewYaw));
               this.pendingTitle = null;
            }

            long songMs = controller.songPositionMs();
            this.updateLines(controller, songMs, now, viewYaw, font);
            this.draw(ctx.poseStack(), ctx.submitNodeCollector(), cam.pos, cam.orientation, mc, songMs, now, dt, time);
         }
      }
   }

   private void updateLines(LyricsController controller, long songMs, long now, float viewYaw, Font font) {
      Lyrics lyrics = controller.lyrics();
      boolean trackPlaying = controller.isPlaying() && controller.currentMedia().hasTrack();

      if (lyrics != this.shownLyrics) {
         if (this.currentLine != null) {
            this.retire(this.currentLine, now);
         }

         if (this.notes != null) {
            this.retire(this.notes, now);
         }

         this.currentLine = null;
         this.notes = null;
         this.shownLyrics = lyrics;
         this.shownIndex = Integer.MIN_VALUE;
      }

      if (lyrics != null) {
         boolean gap;
         if (!lyrics.instrumental() && !lyrics.lines().isEmpty()) {
            List<Lyrics.Line> lines = lyrics.lines();
            int idx = lyrics.indexAt(songMs);
            if (idx != this.shownIndex) {
               if (this.currentLine != null) {
                  this.retire(this.currentLine, now);
               }

               this.currentLine = null;
               if (idx >= 0 && !lines.get(idx).isBlank()) {
                  this.currentLine = this.spawnLyric(lyrics, idx, viewYaw, font);
                  this.texts.add(this.currentLine);
               }

               this.shownIndex = idx;
            }

            if (this.currentLine != null && songMs >= this.currentLine.endSongMs) {
               this.retire(this.currentLine, now);
               this.currentLine = null;
            }

            long nextStart = idx + 1 < lines.size() ? lines.get(idx + 1).timeMs() : Long.MAX_VALUE;
            gap = this.currentLine == null && nextStart != Long.MAX_VALUE && nextStart - songMs > 1500L;
         } else {
            gap = lyrics.instrumental();
         }

         if (gap && this.notes == null) {
            this.notes = this.spawnNotes(font, viewYaw);
            this.texts.add(this.notes);
         } else if (!gap && this.notes != null) {
            this.retire(this.notes, now);
            this.notes = null;
         }
      } else if (trackPlaying) {
         // When lyrics are not available or still loading, keep sky alive with musical notes
         if (this.notes == null) {
            this.notes = this.spawnNotes(font, viewYaw);
            this.texts.add(this.notes);
         }
      } else {
         if (this.notes != null) {
            this.retire(this.notes, now);
            this.notes = null;
         }
      }
   }

   private void draw(PoseStack ps, OrderedSubmitNodeCollector out, Vec3 camPos, Quaternionf rot, Minecraft mc, long songMs, long now, float dt, float time) {
      Vector3f right = rot.transform(new Vector3f(1.0F, 0.0F, 0.0F));
      Vector3f up = rot.transform(new Vector3f(0.0F, 1.0F, 0.0F));
      Font.DisplayMode mode = this.config.seeThroughWalls ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
      Iterator<FloatingText> it = this.texts.iterator();

      while (it.hasNext()) {
         FloatingText ft = it.next();
         if (ft.kind == FloatingText.Kind.TITLE && !ft.retired() && ft.ageMs(now) > 7500L) {
            this.retire(ft, now);
         }

         float exit = ft.exitProgress(now);
         if (exit >= 1.0F) {
            it.remove();
         } else {
            Vec3 pos = this.place(ft, mc, time, dt);
            float skyScaleMult = this.config.skyMode ? 1.55F : 1.0F;
            float s = (float)(0.028F * this.config.textScale * skyScaleMult);
            long local = ft.localTime(songMs, now);
            float exitEase = Anim.easeOutCubic(exit);
            float exitFade = 1.0F - Anim.easeInQuad(exit);
            ps.pushPose();
            ps.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
            ps.mulPose(rot);
            ps.scale(s, -s, s);

            for (FloatingText.Word w : ft.words) {
               float age = (float)(local - w.appearAt);
               if (!(age < 0.0F)) {
                  float x = w.cx + w.driftX * exitEase;
                  float rise = (1.0F - Anim.easeOutCubic(age / 400.0F)) * 9.0F;
                  float wave = !this.config.floatingMotion
                     ? 0.0F
                     : (float)Math.sin(time * 2.4F + w.index * 0.55F + ft.seed) * (ft.kind == FloatingText.Kind.NOTES ? 2.5F : 0.85F);
                  float y = w.cy + rise + wave + w.driftY * exitEase;
                  float z = w.driftZ * exitEase;
                  if (!w.popped && exit == 0.0F) {
                     w.popped = true;
                     if (this.config.particles && age < 350.0F && ft.kind != FloatingText.Kind.NOTES) {
                        Vec3 wp = pos.add(
                           right.x() * x * s - up.x() * y * s, right.y() * x * s - up.y() * y * s, right.z() * x * s - up.z() * y * s
                        );
                        this.popParticles(mc.level, wp, ft, w);
                     }
                  }

                  float alpha = Anim.clamp01(age / 160.0F) * exitFade;
                  if (ft.kind == FloatingText.Kind.NOTES) {
                     alpha *= 0.7F + 0.3F * (float)Math.sin(time * 4.0F + w.index);
                  }

                  if (!(alpha < 0.04F)) {
                     // Cool karaoke active bounce
                     float activePop = 1.0F;
                     if (this.config.coolVisuals && age >= 0.0F && age < 550.0F) {
                        float p = 1.0F - (age / 550.0F);
                        activePop = 1.0F + 0.22F * (float)Math.sin(p * Math.PI);
                     }

                     float k = Anim.easeOutBack(age / 300.0F) * w.scale * (1.0F + 0.25F * exitEase) * activePop;
                     LyricsConfig.ColorStyle style = this.config.colorStyle;
                     boolean rainbow = style.hue < 0.0F;
                     float hue = !rainbow ? style.hue : (ft.kind == FloatingText.Kind.NOTES ? time * 0.15F + w.index * 0.12F : ft.hue + w.index * 0.03F);
                     int base = w.fixedColor >= 0 ? w.fixedColor : Anim.hsv(hue, style.saturation, 1.0F);

                     float shineFactor = (this.config.coolVisuals && age < 500.0F) ? (1.0F - age / 500.0F) : 0.0F;
                     int color = Anim.mixRgb(base, 16777215, Math.max(shineFactor * 0.9F, (1.0F - Anim.clamp01(age / 700.0F)) * 0.85F));

                     int glow = style == LyricsConfig.ColorStyle.WHITE ? 2894908 : Anim.hsv(hue, 0.85F, 0.45F);
                     if (this.config.coolVisuals && age < 500.0F) {
                        glow = Anim.mixRgb(glow, 0xFFE082, (1.0F - age / 500.0F) * 0.6F);
                     }

                     ps.pushPose();
                     ps.translate(x, y, z);
                     ps.scale(k, k, k);
                     float tx = -w.width / 2.0F;
                     if (this.config.seeThroughWalls) {
                        out.submitText(ps, tx, -4.0F, w.seq, false, mode, 15728880, Anim.argb(alpha, color), 0, 0);
                     } else if (this.config.glowOutline) {
                        out.submitText(ps, tx, -4.0F, w.seq, false, mode, 15728880, Anim.argb(alpha, color), 0, Anim.argb(alpha * 0.95F, glow));
                     } else {
                        out.submitText(ps, tx, -4.0F, w.seq, true, mode, 15728880, Anim.argb(alpha, color), 0, 0);
                     }

                     ps.popPose();
                  }
               }
            }

            ps.popPose();
            if (ft.kind == FloatingText.Kind.NOTES && this.config.particles && exit == 0.0F && now - ft.lastNoteParticleNanos > 1300000000L) {
               ft.lastNoteParticleNanos = now;
               mc.level.addParticle(
                  ParticleTypes.NOTE,
                  pos.x + (this.random.nextDouble() - 0.5),
                  pos.y + 0.4,
                  pos.z + (this.random.nextDouble() - 0.5),
                  this.random.nextInt(25) / 24.0,
                  0.0,
                  0.0
               );
            }
         }
      }
   }

   private Vec3 place(FloatingText ft, Minecraft mc, float time, float dt) {
      if (this.config.alwaysInFront) {
         // Smoothly track the player camera look direction so it is ALWAYS in front of player
         float yawDiff = Mth.wrapDegrees(this.viewYaw - ft.yawDeg);
         ft.yawDeg += yawDiff * Math.min(1.0F, dt * 8.0F);

         float targetPitch;
         double dist;
         double height;
         if (this.config.skyMode) {
            // High up in the sky, completely out of the player's crosshair and body
            targetPitch = (float)(-this.config.skyAngle + Math.min(8.0F, Math.max(-8.0F, this.viewPitch * 0.15F)));
            dist = this.config.skyDistance;
            height = this.config.skyHeight + this.config.heightOffset;
         } else {
            targetPitch = Math.max(-8.0F, Math.min(8.0F, this.viewPitch * 0.25F));
            dist = this.config.distance;
            height = this.config.heightOffset - 0.15;
         }

         ft.pitchDeg += (targetPitch - ft.pitchDeg) * Math.min(1.0F, dt * 6.0F);

         double rYaw = Math.toRadians(ft.yawDeg);
         double rPitch = Math.toRadians(ft.pitchDeg);
         Vec3 dir = new Vec3(-Math.sin(rYaw) * Math.cos(rPitch), -Math.sin(rPitch), Math.cos(rYaw) * Math.cos(rPitch));
         Vec3 target = this.viewOrigin.add(dir.scale(dist)).add(0.0, height, 0.0);
         if (this.config.skyMode) {
            // Keep strictly in the sky, never pull down onto the player's face
            ft.worldPos = target;
         } else {
            ft.worldPos = clearOfWalls(mc, this.viewOrigin, target);
         }
      } else {
         if (ft.worldPos == null) {
            ft.worldPos = this.spawnPosition(ft, mc);
         } else if (this.config.followPlayer) {
            ft.worldPos = ft.worldPos.add(this.anchorDelta);
         }
      }

      double bob = this.config.floatingMotion ? Math.sin(time * 1.5F + ft.seed) * 0.08 : 0.0;
      return ft.worldPos.add(0.0, bob, 0.0);
   }

   private Vec3 spawnPosition(FloatingText ft, Minecraft mc) {
      double yaw = Math.toRadians(ft.yawDeg);
      double pitch = Math.toRadians(Math.max(-10.0F, Math.min(10.0F, ft.pitchDeg)));
      Vec3 dir = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
      Vec3 target = this.viewOrigin.add(dir.scale(ft.targetDistance)).add(0.0, ft.height + this.config.heightOffset, 0.0);
      return clearOfWalls(mc, this.viewOrigin, target);
   }

   private static Vec3 clearOfWalls(Minecraft mc, Vec3 from, Vec3 to) {
      HitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
      if (hit.getType() == HitResult.Type.MISS) {
         return to;
      }

      Vec3 d = to.subtract(from);
      double full = d.length();
      double allowed = Math.max(1.2, hit.getLocation().distanceTo(from) - 0.45);
      return full < 1.0E-6 ? to : from.add(d.scale(Math.min(1.0, allowed / full)));
   }

   private float spawnYaw(float viewYaw, int side) {
      float maxOff = Math.max(0.0F, Math.min((float)this.config.spreadDegrees, this.viewHalfWidthDeg * 0.4F));
      return viewYaw + side * maxOff * (0.2F + 0.6F * this.random.nextFloat());
   }

   private float spawnPitch(float spreadFraction) {
      return Math.max(-8.0F, Math.min(8.0F, this.viewPitch * 0.25F));
   }

   private void retire(FloatingText ft, long now) {
      if (!ft.retired()) {
         ft.retireNanos = now;

         for (FloatingText.Word w : ft.words) {
            float side = Math.signum(w.cx == 0.0F ? this.random.nextFloat() - 0.5F : w.cx);
            w.driftX = side * (6.0F + this.random.nextFloat() * 22.0F) + (this.random.nextFloat() - 0.5F) * 10.0F;
            w.driftY = -(8.0F + this.random.nextFloat() * 22.0F);
            w.driftZ = (this.random.nextFloat() - 0.5F) * 30.0F;
         }
      }
   }

   private void popParticles(ClientLevel level, Vec3 p, FloatingText ft, FloatingText.Word w) {
      double sx = (this.random.nextDouble() - 0.5) * 0.02;
      double sz = (this.random.nextDouble() - 0.5) * 0.02;
      level.addParticle(ParticleTypes.FIREWORK, p.x, p.y, p.z, sx, 0.012 + this.random.nextDouble() * 0.015, sz);
      if (this.config.coolVisuals) {
         level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, sx * 0.4, 0.01, sz * 0.4);
      }
      if (ft.kind == FloatingText.Kind.LYRIC && (w.index % 2 == 0 || this.config.coolVisuals)) {
         level.addParticle(ParticleTypes.NOTE, p.x, p.y + 0.35, p.z, ((ft.hue + w.index * 0.04F) % 1.0F + 1.0F) % 1.0F, 0.0, 0.0);
      }
   }

   private FloatingText spawnLyric(Lyrics lyrics, int idx, float viewYaw, Font font) {
      List<Lyrics.Line> lines = lyrics.lines();
      Lyrics.Line line = lines.get(idx);
      long next = idx + 1 < lines.size() ? lines.get(idx + 1).timeMs() : line.timeMs() + 6000L;
      long dur = Math.max(300L, next - line.timeMs());
      List<String> rawTokens = KhmerTextUtil.tokenize(line.text());
      List<FloatingText.Word> words = new ArrayList<>(rawTokens.size());
      int totalWeight = 0;

      for (int i = 0; i < rawTokens.size(); i++) {
         String raw = rawTokens.get(i);
         String shaped = KhmerTextUtil.shape(raw);
         FormattedCharSequence seq = KhmerTextUtil.containsKhmer(raw)
            ? KhmerTextUtil.khmerComponent(shaped).getVisualOrderText()
            : Component.literal(shaped).getVisualOrderText();
         words.add(new FloatingText.Word(i, seq, font.width(seq), 1.0F, -1));
         totalWeight += raw.length() + 2;
      }

      long reveal = Math.min((long)(dur * 0.7), 380L * rawTokens.size());
      if (!lyrics.synced()) {
         reveal = Math.min(reveal, 160L * rawTokens.size());
      }

      int weight = 0;

      for (int i = 0; i < rawTokens.size(); i++) {
         words.get(i).appearAt = line.timeMs() + reveal * weight / Math.max(1, totalWeight);
         weight += rawTokens.get(i).length() + 2;
      }

      wrap(words, font.width(" "));
      layout(words, font.width(" "));
      float yaw = this.config.alwaysInFront ? viewYaw : this.spawnYaw(viewYaw, this.lineCounter % 2 == 0 ? -1 : 1);
      float pitch = this.config.skyMode ? (float)(-this.config.skyAngle) : Math.max(-8.0F, Math.min(8.0F, this.viewPitch * 0.25F));
      float height = this.config.skyMode ? (float)this.config.skyHeight : (this.config.alwaysInFront ? 0.0F : (-0.05F + this.random.nextFloat() * 0.1F));
      double dist = this.config.skyMode ? this.config.skyDistance : this.config.distance;
      float hue = this.trackHue + this.lineCounter * 0.09F;
      this.lineCounter++;
      FloatingText ft = new FloatingText(FloatingText.Kind.LYRIC, words, yaw, height, dist, hue, this.random.nextFloat() * 10.0F);
      ft.pitchDeg = pitch;
      ft.endSongMs = Math.min(next, line.timeMs() + reveal + 4500L);
      return ft;
   }

   private FloatingText spawnTitle(Font font, String title, String artist, float viewYaw) {
      List<FloatingText.Word> words = new ArrayList<>();
      int i = 0;
      List<String> titleTokens = new ArrayList<>();
      titleTokens.add("♫");
      titleTokens.addAll(KhmerTextUtil.tokenize(title));

      for (String t : titleTokens) {
         String shaped = KhmerTextUtil.shape(t);
         MutableComponent c = KhmerTextUtil.containsKhmer(t)
            ? KhmerTextUtil.khmerComponent(shaped).copy().withStyle(ChatFormatting.BOLD)
            : Component.literal(shaped).copy().withStyle(ChatFormatting.BOLD);
         FormattedCharSequence seq = c.getVisualOrderText();
         FloatingText.Word w = new FloatingText.Word(i, seq, font.width(seq), 1.6F, -1);
         w.appearAt = i * 70L;
         w.row = 0;
         words.add(w);
         i++;
      }

      int titleCount = i;
      if (artist != null && !artist.isBlank()) {
         int j = 0;

         for (String t : KhmerTextUtil.tokenize(artist)) {
            String shaped = KhmerTextUtil.shape(t);
            FormattedCharSequence seq = KhmerTextUtil.containsKhmer(t)
               ? KhmerTextUtil.khmerComponent(shaped).getVisualOrderText()
               : Component.literal(shaped).getVisualOrderText();
            FloatingText.Word w = new FloatingText.Word(i, seq, font.width(seq), 0.95F, 13224406);
            w.appearAt = titleCount * 70L + 150L + j * 60L;
            w.row = 1;
            words.add(w);
            i++;
            j++;
         }
      }

      float space = font.width(" ");
      float titleWidth = 0.0F;

      for (FloatingText.Word w : words) {
         if (w.row == 0) {
            titleWidth += (w.width + space) * w.scale;
         }
      }

      if (titleWidth > 304.0F) {
         List<FloatingText.Word> titleWords = words.subList(0, titleCount);
         wrap(titleWords, space);
         int titleRows = 0;

         for (FloatingText.Word w : titleWords) {
            titleRows = Math.max(titleRows, w.row + 1);
         }

         for (FloatingText.Word w : words.subList(titleCount, words.size())) {
            w.row = titleRows;
         }
      }

      layout(words, space);
      float pitch = this.config.skyMode ? (float)(-this.config.skyAngle - 3.0F) : Math.max(-8.0F, Math.min(8.0F, this.viewPitch * 0.25F));
      double dist = (this.config.skyMode ? this.config.skyDistance : this.config.distance) + 1.0;
      float height = this.config.skyMode ? (float)this.config.skyHeight + 1.0F : 0.35F;
      FloatingText ft = new FloatingText(
         FloatingText.Kind.TITLE, words, viewYaw, height, dist, this.trackHue, this.random.nextFloat() * 10.0F
      );
      ft.pitchDeg = pitch;
      return ft;
   }

   private FloatingText spawnNotes(Font font, float viewYaw) {
      List<FloatingText.Word> words = new ArrayList<>();
      String[] glyphs = new String[]{"♪", "♫", "♬", "♫", "♪"};

      for (int i = 0; i < glyphs.length; i++) {
         FormattedCharSequence seq = Component.literal(glyphs[i]).getVisualOrderText();
         FloatingText.Word w = new FloatingText.Word(i, seq, font.width(seq), 1.7F, -1);
         w.appearAt = i * 180L;
         words.add(w);
      }

      layout(words, font.width(" ") * 2);
      float pitch = this.config.skyMode ? (float)(-this.config.skyAngle) : Math.max(-8.0F, Math.min(8.0F, this.viewPitch * 0.25F));
      double dist = this.config.skyMode ? this.config.skyDistance : this.config.distance;
      float height = this.config.skyMode ? (float)this.config.skyHeight : 0.05F;
      FloatingText ft = new FloatingText(FloatingText.Kind.NOTES, words, viewYaw, height, dist, this.trackHue, this.random.nextFloat() * 10.0F);
      ft.pitchDeg = pitch;
      return ft;
   }

   private static void wrap(List<FloatingText.Word> words, float space) {
      float total = 0.0F;

      for (FloatingText.Word w : words) {
         total += w.width * w.scale;
      }

      total += space * Math.max(0, words.size() - 1);
      int rows = Math.max(1, (int)Math.ceil(total / 190.0F));
      float target = total / rows;
      int row = 0;
      float rowWidth = 0.0F;

      for (FloatingText.Word w : words) {
         float ww = w.width * w.scale;
         if (rowWidth > 0.0F && rowWidth + space + ww / 2.0F > target && row < rows - 1) {
            row++;
            rowWidth = 0.0F;
         }

         w.row = row;
         rowWidth += (rowWidth > 0.0F ? space : 0.0F) + ww;
      }
   }

   private static void layout(List<FloatingText.Word> words, float space) {
      int rows = 0;

      for (FloatingText.Word w : words) {
         rows = Math.max(rows, w.row + 1);
      }

      float[] rowWidth = new float[rows];
      float[] rowHeight = new float[rows];
      boolean[] started = new boolean[rows];

      for (FloatingText.Word w : words) {
         if (started[w.row]) {
            rowWidth[w.row] = rowWidth[w.row] + space * w.scale;
         }

         rowWidth[w.row] = rowWidth[w.row] + w.width * w.scale;
         rowHeight[w.row] = Math.max(rowHeight[w.row], 12.0F * w.scale);
         started[w.row] = true;
      }

      float totalHeight = 0.0F;

      for (float h : rowHeight) {
         totalHeight += h;
      }

      float[] cursor = new float[rows];
      float[] rowCenterY = new float[rows];
      float y = -totalHeight / 2.0F;

      for (int r = 0; r < rows; r++) {
         cursor[r] = -rowWidth[r] / 2.0F;
         rowCenterY[r] = y + rowHeight[r] / 2.0F;
         y += rowHeight[r];
      }

      for (FloatingText.Word w : words) {
         float ww = w.width * w.scale;
         w.cx = cursor[w.row] + ww / 2.0F;
         w.cy = rowCenterY[w.row];
         cursor[w.row] = cursor[w.row] + (ww + space * w.scale);
      }
   }
}
