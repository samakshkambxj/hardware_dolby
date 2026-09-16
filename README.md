# Lunaris Dolby for Nothing Phone (3a) Lite

Dolby Atmos (DAX3 `3.7.0.8_r1`) port for **Nothing Phone (3a) Lite (Galaxian)**, with the
Compose-based **Lunaris Dolby** control app as the front-end.

- Pinned blobs from **Galaxian, Android 17**
- **Tested only on `Galaxian`** — other devices may work but are unsupported

## Features

### Core Dolby audio

- Dolby DAX3 DS2 processing (`libswdap`, `libswgamedap`, `libswvqe`)
- AC-4 / DD+ decoding via Codec2 (`libcodec2_soft_ac4dec`, `libcodec2_soft_ddpdec`)
- `media_codecs_dolby_audio.xml` + C2 services wired through `dolby.mk`
- Optional Dolby Vision (`TARGET_INCLUDES_DolbyVision`) and OEM Moto Dolby app (`TARGET_INCLUDES_OEM_App`)
- DSP volume synchronizer (`DSPVolumeSynchronizer`) and conflict-package remover (`RemovePackagesDolby`)
- Head-tracker permission + spatializer props preconfigured (`ro.audio.spatializer_enabled`, `ro.audio.headtracking_enabled`, etc.)

### Lunaris Dolby app (Compose rewrite)

Modern 4-tab pager UI: **Settings / Equalizer / Advanced / Volume**, with floating blurred nav toolbar,
first-run onboarding tutorial, and an About page with version, links, and credits.

### Sound tuning

- 7 profiles (Dynamic, Movie, Music, Game, Work, Casual, Mood) with animated 3D icons
- Intelligent graphic equalizer with interactive frequency-response curve, live spectrum, and preset import/export
- Bass / mid / treble enhancement plus sub / mid / upper bass trims on top of master bass
- Surround virtualizer + standalone stage-width slider with auto-virtualizer
- Dialogue enhancer, volume leveler (+ leveler-strength slider), multiband dynamics EQ with per-band resets
- AutoEQ headphone correction profiles with search, download cache, and one-tap apply
- Live landing waveform + dynamics visualizer that follows real playback
- Audio output picker (speaker / wired / BT / etc.) with 3D animated device icons
- System-wide channel-balance control

### Scenes, per-app profiles & automation

- One-tap scene presets (Movie Night, Bass Boost, Podcast, Gaming, + more) with scene reset
- Custom scenes + per-device scenes with auto-apply (remembers state per output device)
- Per-app audio profiles with background monitor service and auto-apply on app launch
- Sleep timer (exact-alarm) with notification-listener integration
- Broadcast automation API for Tasker / MacroDroid / scripts (`TOGGLE`, `SET_ENABLED`, `SET_PROFILE`, `APPLY_SCENE`)
- Quick Settings tiles: Dolby master, Scene cycle, Leveler
- DAP probe screen for inspecting the live Dolby effect state

### Spatial audio

- In-app spatializer / head-tracking toggles driving the framework `Spatializer`
- Transaural / stereo-spatialization defaults handled via `dolby.mk` props

### Personalization & polish

- Page Style customization, dense floating-particles background, bouncy edge-stretch + spring animations
- Page Style extras: card style (Filled/Outlined/Elevated), icon shape + size, header banner toggles,
  centered header, particle density, navbar blur toggle, dynamic color / AMOLED black / 8 accents,
  and 4 switchable launcher icons (Dynamic, Indigo, Midnight, Gold)
- Navbar-only live blur, blurred dialogs, themed toasts, contributor avatars
- Hidden easter eggs

## Getting Started

For dolby media codecs to work add this line in your media codecs config (should be in vendor partition) and make sure your device supports c2 codecs. :-

```bash
<Include href="media_codecs_dolby_audio.xml" />
```

To build, add the dolby effects in your device's audio effects config then inherit the dolby config by adding this in your device's makefile :-

```bash
$(call inherit-product, hardware/dolby/dolby.mk)
```

Optional flags (in your device makefile / BoardConfig):

```bash
# Include Motorola OEM Dolby app + permissions
TARGET_INCLUDES_OEM_App := true

# Include Dolby Vision HAL + C2 components
TARGET_INCLUDES_DolbyVision := true
```

Now, moving hidl definitions in manifest to device trees is completely absurd so stop overriding manifest in your device trees an example for such would be :-

Changing these in BoardConfig makefile of your device tree:-

```bash
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE :=
```
And

```bash
DEVICE_MANIFEST_FILE :=
```

To:-

```bash
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE +=
```
And

```bash
DEVICE_MANIFEST_FILE +=
```

The only change done above is changing := symbol to += so that manifest can't be overriden from device tree in BoardConfig makefile.

At the end an example commit to properly implement it in your device tree could be :-

* [Galaxian: Integrate Dolby Atmos](https://github.com/samakshkambxj/device_nothing_Galaxian/commit/137e6cec853a2c17be4b1c591504363e9dbc6b77)
* [Galaxian: Dolby: Enable Lunaris Dolby UI Package](https://github.com/samakshkambxj/device_nothing_Galaxian/commit/fb6b53fda1f0a1e458f605e6df8be68e86385174)

Automation example (adb):

```bash
adb shell am broadcast -n org.lunaris.dolby/.service.DolbyCommandReceiver \
  -a org.lunaris.dolby.action.SET_PROFILE --ei profile 1
```

## Changelog

### 2026-09-16

- Keep background particles below the top bar
- Add Page Style customization
- Add audio output picker with 3D animated device icons
- Unify padding and back fade animation
- Add navbar-only real blur, fix lag/delay
- Add About page with app info, links and credits entry
- Fix slider post-release drift, spectrum lingering, gate MBC/limiter resets, restore system toasts
- Restore profile saturation, fix top-bar padding, add contributor avatars
- Drop inert widening/leveler sliders, restore stock virtualizer gating
- Fix dynamics spectrum level, add dynamics resets, fix waveform stuck state, opaque output card
- Add sub/mid/upper bass trims on top of master bass
- Animate nav-pill icons like the equalizer icon
- Restore virtualizer sliders, fix slider stutter, theme toasts, new easter eggs
- Leveler strength slider and DAP probe screen
- Standalone stage-width slider with auto virtualizer
- Colorful 3D animated profile icons
- Fix edge-stretch getting stuck after slow lift
- Add dense floating particles background
- Easter eggs, dynamics persistence, spectrum curve
- Fix random crash when scrolling back up
- Fix DynamicsEqualizerViewModel init-order NPE
- Landing waveform follows real playback
- First-run tutorial
- Bouncy spring animations and edge stretch
- Blur background behind dialogs
- Improved equalizer

### 2026-09-15

- Add spatial audio toggles
- Add per-device scenes with auto-apply
- Add scene-cycle and leveler QS tiles
- Add scene reset option and more built-in scenes
- Sync volume panel with system volume and fix padding
- Add automation broadcast commands
- Add system-wide channel balance control

### 2026-09-14

- Add sleep timer
- Add one-tap scene presets
- Add volume controls

### 2026-09-06

- Initial LunarisDolby import
- Enable R8 optimization
- Implement swipe navigation between main screens
- Implement AutoEQ headphone correction profiles
- Apply Dolby profiles only to compatible audio
- Fix headphone surround slider stuck at 35
- Show active audio output device on home screen
- Use CLICK haptic instead of DOUBLE_CLICK
- Add prebuilt_etc for preinstalled packages

## Credits

* Samakshhhh ([samakshkambxj](https://github.com/samakshkambxj)) — Port Lunaris Dolby for Nothing Phone (3a) Lite (Galaxian) and adds new features
* Ghost ([Ghosuto](https://github.com/Ghosuto)) — Rewrite Dolby in Compose (Lunaris Dolby)
* Anshuman X ([maxxcodebug](https://github.com/maxxcodebug)) — Volume panel
* Adithya R ([adithya2306](https://github.com/adithya2306)) — AOSPA Dolby Manager (Initial Code)
* Kenway ([kenway214](https://github.com/kenway214)) — Base & Treble Changes, EQ Tuning
* tranQuila ([MrTopia](https://github.com/MrTopia)) — Adding per-device dolby state memory
* Pablo Escobar ([pabloescobar-reborn](https://github.com/pabloescobar-reborn)) — AutoEQ headphone correction profiles
* swiitch-OFF-Lab ([swiitch-OFF-Lab](https://github.com/swiitch-OFF-Lab)) — Base tree ([hardware_dolby](https://github.com/swiitch-OFF-Lab/hardware_dolby))
