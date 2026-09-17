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
- Collapsible per-band rows in dynamics processing (EQ bands + MBC, collapsed by default)
- Framework FX tab: bass boost, virtualizer, preset reverb and loudness enhancer on the output mix
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

- Customization section (formerly Page Style), dense floating-particles background, bouncy edge-stretch + spring animations
- Customization extras: card style (Filled/Outlined/Elevated, honored on every page including Equalizer),
  icon shape + size, header banner toggles, centered header, particle density,
  transparent/frosted navbar style with retained live blur, dynamic color / AMOLED black / 8 accents,
  and 4 switchable launcher icons (Dynamic, Indigo, Midnight, Gold)
- Pill-shaped UI throughout (PILL corners by default, stadium rows/tiles/chips), spinning Dolby logo
  loader in the decoders section, blurred dialogs, themed toasts, contributor avatars
- Hidden easter eggs

## Getting Started

### 1. Dolby media codecs

For dolby media codecs to work add this line in your media codecs config (should be in vendor partition) and make sure your device supports c2 codecs. :-

```bash
<Include href="media_codecs_dolby_audio.xml" />
```

### 2. Inherit the Dolby config

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

### 3. Enable the Lunaris Dolby app

Add the control app to your device makefile so it gets built and installed :-

```bash
# LunarisDolby
PRODUCT_PACKAGES += \
    LunarisDolby
```

### 4. Enable spatial audio

Spatial audio needs the spatializer effect, a spatial output mix port, and the feature flag in your
device tree (example: Galaxian):

- [Galaxian: Enable Dolby Spatial Audio](https://github.com/samakshkambxj/device_nothing_Galaxian/commit/902cdede1dd542ef7b5d5b7384af7a29b64ba009) —
  adds the spatializer effect (`libswspatializer`, uuid `ccd4cf09-…`) to `audio_effects.xml`,
  adds a `spatial output` mix port (PCM_16_BIT / 48000 / stereo, `AUDIO_OUTPUT_FLAG_SPATIALIZER`)
  routed to Speaker, Wired Headset/Headphones and USB, and advertises the
  `android.hardware.audio.spatializer` feature.
- [Galaxian: Route spatial output to BT A2DP](https://github.com/samakshkambxj/device_nothing_Galaxian/commit/47cacfab32c6ca731ca98ad2c76ce1197271fe00) —
  adds the same spatial mix port inside the **Bluetooth** HAL module via a local
  `bluetooth_audio_policy_configuration.xml` and routes it to the BT A2DP sinks.
  Do NOT declare BT A2DP devices inside primary on MTK — APM will open A2DP via the
  wrong HAL and break all BT audio.

### 5. Don't override the VINTF manifest

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

The only change done above is changing := symbol to += so that manifest can't be overriden from device tree in BoardConfig makefile (this is also what lets the `dolby.mk` VINTF fragments merge instead of being dropped).

At the end an example commit to properly implement it in your device tree could be :-

* [Galaxian: Integrate Dolby Atmos](https://github.com/samakshkambxj/device_nothing_Galaxian/commit/137e6cec853a2c17be4b1c591504363e9dbc6b77)
* [Galaxian: Dolby: Enable Lunaris Dolby UI Package](https://github.com/samakshkambxj/device_nothing_Galaxian/commit/fb6b53fda1f0a1e458f605e6df8be68e86385174)

Automation example (adb):

```bash
adb shell am broadcast -n org.lunaris.dolby/.service.DolbyCommandReceiver \
  -a org.lunaris.dolby.action.SET_PROFILE --ei profile 1
```

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## Credits

* Samakshhhh ([samakshkambxj](https://github.com/samakshkambxj)) — Port Lunaris Dolby for Nothing Phone (3a) Lite (Galaxian) and adds new features
* Ghost ([Ghosuto](https://github.com/Ghosuto)) — Rewrite Dolby in Compose (Lunaris Dolby)
* Anshuman X ([maxxcodebug](https://github.com/maxxcodebug)) — Volume panel
* Adithya R ([adithya2306](https://github.com/adithya2306)) — AOSPA Dolby Manager (Initial Code)
* Kenway ([kenway214](https://github.com/kenway214)) — Base & Treble Changes, EQ Tuning
* tranQuila ([MrTopia](https://github.com/MrTopia)) — Adding per-device dolby state memory
* Pablo Escobar ([pabloescobar-reborn](https://github.com/pabloescobar-reborn)) — AutoEQ headphone correction profiles
* swiitch-OFF-Lab ([swiitch-OFF-Lab](https://github.com/swiitch-OFF-Lab)) — Base tree ([hardware_dolby](https://github.com/swiitch-OFF-Lab/hardware_dolby))
