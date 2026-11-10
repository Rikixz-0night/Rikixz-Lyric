package com.realtimelyrics.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Anim {
   private Anim() {
   }

   public static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   public static float easeOutCubic(float t) {
      float u = 1.0F - clamp01(t);
      return 1.0F - u * u * u;
   }

   public static float easeInQuad(float t) {
      t = clamp01(t);
      return t * t;
   }

   public static float easeOutBack(float t) {
      t = clamp01(t);
      float c1 = 1.9F;
      float c3 = c1 + 1.0F;
      float u = t - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   public static int hsv(float h, float s, float v) {
      h -= (float)Math.floor(h);
      int i = (int)(h * 6.0F);
      float f = h * 6.0F - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - f * s);
      float t = v * (1.0F - (1.0F - f) * s);
      float r;
      float g;
      float b;
      switch (i % 6) {
         case 0:
            r = v;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = v;
            b = p;
            break;
         case 2:
            r = p;
            g = v;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = v;
            break;
         case 4:
            r = t;
            g = p;
            b = v;
            break;
         default:
            r = v;
            g = p;
            b = q;
      }

      return (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
   }

   public static int mixRgb(int a, int b, float t) {
      t = clamp01(t);
      int r = (int)((a >> 16 & 0xFF) * (1.0F - t) + (b >> 16 & 0xFF) * t);
      int g = (int)((a >> 8 & 0xFF) * (1.0F - t) + (b >> 8 & 0xFF) * t);
      int bl = (int)((a & 0xFF) * (1.0F - t) + (b & 0xFF) * t);
      return r << 16 | g << 8 | bl;
   }

   public static int argb(float alpha, int rgb) {
      return (int)(clamp01(alpha) * 255.0F) << 24 | rgb & 16777215;
   }
}
