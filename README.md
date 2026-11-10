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

---

## ✨ Features

- ☁️ **Sky Lyrics Mode:** Lyrics elevate seamlessly into the sky (+26° pitch angle) and track your horizontal look direction, ensuring your crosshair and character are always clear.
- ♫ **Draggable Glassmorphic HUD:** A live music overlay card displaying the mod logo/artwork, song title, sub-song (artist & album), real-time karaoke sub-lyrics, an animated 5-bar equalizer, and a glowing progress bar.
- 🖱️ **Visual GUI & Interactive Drag Editor:** Press **`H`** in-game to drag and position your music HUD anywhere on screen with magnetic center snapping guides and scale adjustments (50% – 200%).
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
build/libs/rikixz-lyric-1.0.0.jar
```

---

## 📜 Credits & Open Source Acknowledgements

- **Rikixz:** Port to Minecraft 26.2, Sky Mode view tracking, HarfBuzz Khmer font shaper, Visual GUI & drag-and-drop HUD, multi-player Linux MPRIS engine, and visual effects.
- **Real-Time Lyrics:** Based on the original open-source [real-time-lyrics](https://github.com/mikhaildn/real-time-lyrics) mod concept.
- **LRCLIB:** Powered by [LRCLIB](https://lrclib.net/) for synchronized lyrics fetching.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
