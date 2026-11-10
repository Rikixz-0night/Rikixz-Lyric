package com.realtimelyrics.media;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface MediaSource {
   MediaSample latest();

   void start();

   void stop();

   static MediaSource forThisOs() {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      if (os.contains("win")) {
         return new WindowsMediaSource();
      } else {
         return os.contains("mac") ? new MacMediaSource() : new PlayerctlMediaSource();
      }
   }
}
