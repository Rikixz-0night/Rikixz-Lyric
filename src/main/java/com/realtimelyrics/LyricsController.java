package com.realtimelyrics;

import com.realtimelyrics.lyrics.LrcLibClient;
import com.realtimelyrics.lyrics.Lyrics;
import com.realtimelyrics.lyrics.TitleCleaner;
import com.realtimelyrics.media.MediaSample;
import com.realtimelyrics.media.MediaSource;
import com.realtimelyrics.render.LyricsRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class LyricsController {
   private static final long CORRECTION_THRESHOLD_MS = 400L;
   private final LyricsConfig config;
   private final MediaSource source;
   private final LrcLibClient lrclib = new LrcLibClient();
   private final LyricsRenderer renderer;
   private long anchorMs;
   private long anchorNanos = System.nanoTime();
   private boolean playing;
   private String trackKey;
   private boolean positionKnown;
   private long nudgeMs;
   private long noMediaSinceNanos = -1L;
   private Lyrics lyrics;
   private String displayTitle = "";
   private String displayArtist = "";

   public LyricsController(LyricsConfig config, MediaSource source, LyricsRenderer renderer) {
      this.config = config;
      this.source = source;
      this.renderer = renderer;
   }

   public Lyrics lyrics() {
      return this.lyrics;
   }

   public boolean isPlaying() {
      return this.playing;
   }

   public String displayTitle() {
      return this.displayTitle;
   }

   public String displayArtist() {
      return this.displayArtist;
   }

   public MediaSample currentMedia() {
      MediaSample s = this.source.latest();
      return s != null ? s : MediaSample.none();
   }

   public long songPositionMs() {
      long pos = this.anchorMs + (this.playing ? (System.nanoTime() - this.anchorNanos) / 1000000L : 0L);
      return pos + this.config.globalOffsetMs + this.nudgeMs;
   }

   private long rawPosition(long nowNanos) {
      return this.anchorMs + (this.playing ? (nowNanos - this.anchorNanos) / 1000000L : 0L);
   }

   private void setClock(long positionMs, boolean isPlaying, long nowNanos) {
      this.anchorMs = positionMs;
      this.anchorNanos = nowNanos;
      this.playing = isPlaying;
   }

   public void tick(Minecraft mc) {
      long now = System.nanoTime();
      MediaSample s = this.source.latest();
      if (s != null && s.hasTrack()) {
         this.noMediaSinceNanos = -1L;
         String key = s.app() + "\n" + s.artist() + "\n" + s.title();
         if (!key.equals(this.trackKey)) {
            this.startTrack(mc, s, key, now);
         } else {
            if (s.positionKnown()) {
               long expected = s.positionAt(now);
               if (!this.positionKnown || s.playing() != this.playing || Math.abs(expected - this.rawPosition(now)) > 400L) {
                  this.setClock(expected, s.playing(), now);
               }

               this.positionKnown = true;
            } else if (s.playing() != this.playing) {
               this.setClock(this.rawPosition(now), s.playing(), now);
            }
         }
      } else {
         if (this.trackKey != null) {
            if (this.noMediaSinceNanos < 0L) {
               this.noMediaSinceNanos = now;
            } else if (now - this.noMediaSinceNanos > 3000000000L) {
               this.clearTrack();
            }
         }
      }
   }

   private void clearTrack() {
      this.trackKey = null;
      this.lyrics = null;
      this.renderer.clear();
   }

   private void startTrack(Minecraft mc, MediaSample s, String key, long now) {
      this.trackKey = key;
      this.lyrics = null;
      this.nudgeMs = 0L;
      this.positionKnown = s.positionKnown();
      this.setClock(this.positionKnown ? s.positionAt(now) : 0L, s.playing(), now);
      String[] display = TitleCleaner.display(s.artist(), s.title());
      this.displayArtist = display[0];
      this.displayTitle = display[1];
      this.renderer.onNewTrack(this.displayTitle, this.displayArtist);
      RealTimeLyricsClient.LOG.info(
         "Now playing: {} - {} ({}, position {})",
         this.displayArtist,
         this.displayTitle,
         s.app(),
         this.positionKnown ? "reported" : "not reported"
      );
      this.status(
         mc,
         Component.literal("♫ Finding lyrics for ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(com.realtimelyrics.render.KhmerTextUtil.shape(this.displayTitle)).withStyle(ChatFormatting.GOLD))
      );
      this.lrclib
         .find(s.artist(), s.title(), s.album(), s.durationMs())
         .whenComplete(
            (result, err) -> mc.execute(
               () -> {
                  if (key.equals(this.trackKey)) {
                     if (err == null && result != null && !result.isEmpty()) {
                        this.lyrics = result.get();
                        RealTimeLyricsClient.LOG.info(
                           "Lyrics found for {}: {} lines, synced={}, instrumental={}",
                           this.displayTitle,
                           this.lyrics.lines().size(),
                           this.lyrics.synced(),
                           this.lyrics.instrumental()
                        );
                        if (this.lyrics.instrumental()) {
                           this.status(mc, Component.literal("♪ Instrumental").withStyle(ChatFormatting.AQUA));
                        } else if (!this.lyrics.synced()) {
                           this.status(mc, Component.literal("Only unsynced lyrics found — timing is a guess").withStyle(ChatFormatting.YELLOW));
                        } else if (!this.positionKnown) {
                           this.status(
                              mc,
                              Component.literal("♫ Lyrics ready · this player doesn't share its position, timed from song start — use [ ] to sync")
                                 .withStyle(ChatFormatting.GREEN)
                           );
                        } else {
                           this.status(mc, Component.literal("♫ Lyrics synced").withStyle(ChatFormatting.GREEN));
                        }
                     } else {
                        RealTimeLyricsClient.LOG.info("No lyrics found for {} - {}", this.displayArtist, this.displayTitle);
                        this.status(
                           mc,
                           Component.literal("✖ No lyrics found for ")
                              .withStyle(ChatFormatting.GRAY)
                              .append(Component.literal(com.realtimelyrics.render.KhmerTextUtil.shape(this.displayTitle)).withStyle(ChatFormatting.GOLD))
                        );
                     }
                  }
               }
            )
         );
   }

   public void nudge(Minecraft mc, long deltaMs) {
      this.nudgeMs += deltaMs;
      long total = this.nudgeMs + this.config.globalOffsetMs;
      this.status(mc, Component.literal(String.format("Lyrics timing: %+.1fs", total / 1000.0)).withStyle(ChatFormatting.GREEN));
   }

   public void restartTiming(Minecraft mc) {
      long now = System.nanoTime();
      this.setClock(0L, true, now);
      this.nudgeMs = 0L;
      this.status(mc, Component.literal("Lyrics timing reset to the start of the song").withStyle(ChatFormatting.GREEN));
   }

   public void status(Minecraft mc, Component message) {
      if (this.config.statusMessages && mc.player != null) {
         mc.player.sendOverlayMessage(message);
      }
   }
}
