<p align="center">
  <img src="src/main/resources/assets/rikixz_lyric/icon.png" width="160" height="160" alt="Rikixz Lyric Logo" style="border-radius: 24px;" />
</p>

<h1 align="center">♫ Rikixz Lyric</h1>

<p align="center">
  <b>Real-time synced lyrics & draggable music HUD in Minecraft with Sky Mode, Khmer font ligatures, and rich visual effects!</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-26.2-brightgreen?style=for-the-badge&logo=minecraft" alt="Minecraft 26.2" />
  <img src="https://img.shields.io/badge/Modloader-Fabric-blue?style=for-the-badge" alt="Fabric" />
  <img src="https://img.shields.io/badge/License-MIT-orange?style=for-the-badge" alt="MIT License" />
  <img src="https://img.shields.io/badge/Author-Rikixz-purple?style=for-the-badge" alt="Rikixz" />
</p>

---

## 🌟 Overview

**Rikixz Lyric** brings your real-world music into Minecraft with real-time synchronized karaoke lyrics floating in the sky and a draggable, glassmorphic "Now Playing" HUD overlay!

Supports **Spotify, YouTube, Firefox, Chrome, VLC**, and other MPRIS/Windows media players on both Linux and Windows.

[**Download the latest build**](https://rikixz-api.web.app/downloads/Rikixz.Lyric.jar) · [Visit the website](https://rikixz-api.web.app) · [View the changelog](https://rikixz-api.web.app/#changelog)

---

## ✨ Features

- ☁️ **Sky Lyrics Mode:** Lyrics elevate seamlessly into the sky (+26° pitch angle) and track your horizontal look direction, ensuring your crosshair and character are always clear.
- ♫ **Midnight-Glass HUD:** A 300 × 100 Now Playing card with rounded artwork, a scaled song title, artist and album, a live karaoke line, an animated 5-bar equalizer, and a gradient progress bar with timestamps.
- 🖱️ **Visual GUI & Drag Editor:** Press **`H`** in-game to move the HUD with snap guides, top/bottom presets, and a live scale slider (50% – 200%). The settings dashboard, buttons, sliders, and toggles share the website's lavender-and-mint glass design.
- 🇰🇭 **Khmer Font & HarfBuzz Layout Engine:** Full support for Khmer syllables, sub-consonants (Coeng ្), and complex vowel ligatures without broken or misplaced glyphs (`NotoSansKhmer-Bold.ttf`).
- 🎨 **Karaoke Word Effects & Themes:** Active word-by-word bloom pop, multiple color styles (*Rainbow, Neon Cyan, Sunset, Emerald, Sakura, Gold/Amber, White*), gentle floating wave bobbing, and note particle sparkles.
- ⚡ **Smart Multi-Player Detection:** Prioritizes actively playing media across Spotify, Firefox, Chrome, and local players while ignoring background chat voice notes.

---

## 🎮 Keybindings & Controls

| Key | Action |
|:---:|---|
| <kbd>H</kbd> | **Open HUD Position Editor** (Drag & drop music card, adjust scale, snap to center) |
| <kbd>K</kbd> | **Toggle in-world lyrics visibility** ON / OFF |
| <kbd>[</kbd> | Nudge lyrics 0.1s sooner |
| <kbd>]</kbd> | Nudge lyrics 0.1s later |
| <kbd>\\</kbd> | Reset song playback timer |
| **Mod Menu** | Open full visual configuration dashboard |

---

## 🛠️ Building From Source

To build the mod from source, ensure you have **Java 21+** (or Java 25) installed:

```bash
git clone https://github.com/Rikixz-0night/Rikixz-Lyric.git
cd Rikixz-Lyric
./gradlew build
```

The compiled mod JAR will be located at:
```text
build/libs/rikixz-lyric-1.1.0.jar
```

---

## 📋 Changelog

### 1.1.0 — November 13, 2026

- Rebuilt the HUD as a 300 × 100 midnight-glass card with rounded artwork, a larger title, karaoke line, equalizer, gradient progress bar, and timestamps.
- Redesigned the config dashboard and HUD placement editor with website-style glass panels, snap guides, presets, corner brackets, a scale slider, and a live preview.
- Restyled the buttons, sliders, and toggles with rounded surfaces, accent rails, and lavender/mint gradients.
- Fixed Khmer text by restoring the vanilla font providers and adding Noto Sans Khmer as a fallback instead of overriding the entire game font. Complex syllables and coeng consonants now render alongside Latin and CJK glyphs.
- Downloads now point to the [Rikixz Lyric website](https://rikixz-api.web.app/downloads/Rikixz.Lyric.jar).

### 1.0.0 — November 11, 2026

- First public release with Sky Mode lyrics, the draggable HUD, LRCLIB sync, karaoke effects, seven themes, multi-player detection, and keybinds.

---

## 📜 Credits & Open Source Acknowledgements

- **Rikixz:** Port to Minecraft 26.2, Sky Mode view tracking, HarfBuzz Khmer font shaper, Visual GUI & drag-and-drop HUD, multi-player Linux MPRIS engine, and visual effects.
- **Real-Time Lyrics:** Based on the original open-source [real-time-lyrics](https://github.com/mikhaildn/real-time-lyrics) mod concept.
- **LRCLIB:** Powered by [LRCLIB](https://lrclib.net/) for synchronized lyrics fetching.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
