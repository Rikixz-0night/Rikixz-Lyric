package com.realtimelyrics.render;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
final class FloatingText {
   static final long EXIT_MS = 900L;
   final FloatingText.Kind kind;
   final List<FloatingText.Word> words;
   float yawDeg;
   final float height;
   final double targetDistance;
   final float hue;
   final float seed;
   final long spawnNanos = System.nanoTime();
   Vec3 worldPos;
   float pitchDeg;
   long retireNanos = -1L;
   long endSongMs = Long.MAX_VALUE;
   long lastNoteParticleNanos;

   FloatingText(FloatingText.Kind kind, List<FloatingText.Word> words, float yawDeg, float height, double targetDistance, float hue, float seed) {
      this.kind = kind;
      this.words = words;
      this.yawDeg = yawDeg;
      this.height = height;
      this.targetDistance = targetDistance;
      this.hue = hue;
      this.seed = seed;
   }

   boolean retired() {
      return this.retireNanos >= 0L;
   }

   float exitProgress(long nowNanos) {
      return this.retireNanos < 0L ? 0.0F : (float)(nowNanos - this.retireNanos) / 1000000.0F / 900.0F;
   }

   long localTime(long songMs, long nowNanos) {
      return this.kind == FloatingText.Kind.LYRIC ? songMs : (nowNanos - this.spawnNanos) / 1000000L;
   }

   long ageMs(long nowNanos) {
      return (nowNanos - this.spawnNanos) / 1000000L;
   }

   @Environment(EnvType.CLIENT)
   enum Kind {
      LYRIC,
      TITLE,
      NOTES;
   }

   @Environment(EnvType.CLIENT)
   static final class Word {
      final int index;
      final FormattedCharSequence seq;
      final float width;
      final float scale;
      final int fixedColor;
      int row;
      float cx;
      float cy;
      long appearAt;
      boolean popped;
      float driftX;
      float driftY;
      float driftZ;

      Word(int index, FormattedCharSequence seq, float width, float scale, int fixedColor) {
         this.index = index;
         this.seq = seq;
         this.width = width;
         this.scale = scale;
         this.fixedColor = fixedColor;
      }
   }
}
