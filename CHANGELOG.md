# Changelog

All notable changes to Rikixz Lyric are documented here. The website mirrors this list at [rikixz-api.web.app/#changelog](https://rikixz-api.web.app/#changelog).

## 1.1.0 — November 13, 2026

### Redesigned HUD

- New 300 × 100 midnight-glass card inspired by the website's hero music card.
- Rounded artwork frame, scaled song title, artist and album line, karaoke lyric, animated equalizer, gradient progress bar, and timestamps.
- Glass layers, aurora glow, corner accents, signal pills, and empty/preview states.

### Website-matched interface

- Rebuilt the settings dashboard with a responsive two-column layout, status header, tabs, hints, footer, and a live preview rail.
- Rebuilt the HUD position editor with a glass backdrop, dot grid, snap guides, drag tag, corner brackets, scale slider, always-visible toggle, top/bottom presets, and readout.
- Restyled buttons, sliders, and toggles with rounded surfaces, lavender/mint gradients, circular thumbs, and hover glow.

### Khmer rendering

- Restored the vanilla `minecraft:font/default` providers instead of replacing them with a Khmer-only font.
- Added Noto Sans Khmer as a fallback before unifont, so shaped Khmer PUA glyphs, Latin, and CJK text all render together.
- Applied Khmer shaping to the config header, HUD editor header, and album line.

### Downloads

- The latest JAR is served directly from [rikixz-api.web.app/downloads/Rikixz.Lyric.jar](https://rikixz-api.web.app/downloads/Rikixz.Lyric.jar).

## 1.0.0 — November 11, 2026

### Initial release

- Sky Mode lyrics that follow your horizontal look direction.
- Draggable Now Playing HUD with artwork, metadata, karaoke lyrics, equalizer, and progress bar.
- Real-time lyrics from LRCLIB with karaoke word effects and seven color themes.
- Spotify, browser, VLC, MPRIS, and Windows media detection.
- Keybinds for HUD placement, lyric visibility, sync nudges, and timer reset.
