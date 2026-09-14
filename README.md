# Lunaris Dolby For Nothing Phone (3a) Lite

Pinned from **Nothing Phone (3a) Lite (Galaxian)**.

All proprietary files are sourced from:

* Android 17
* Variant: `Galaxian`

**Dolby has only been tested on `Galaxian`.**

## Credits

* Samakshhhh ([samakshkambxj](https://github.com/samakshkambxj)) — Port Lunaris Dolby for Nothing Phone (3a) Lite (Galaxian)
* Ghost ([Ghosuto](https://github.com/Ghosuto)) — Rewrite Dolby in Compose (Lunaris Dolby)
* Anshuman X ([maxxcodebug](https://github.com/maxxcodebug)) — Lunaris Dolby port for CMF Phone 1 (Tetris), volume controls - from which this port is inspired.
* Adithya R ([adithya2306](https://github.com/adithya2306)) — AOSPA Dolby Manager (Initial Code)
* Kenway ([kenway214](https://github.com/kenway214)) — Base & Treble Changes, EQ Tuning
* tranQuila ([MrTopia](https://github.com/MrTopia)) — Adding per-device dolby state memory
* Pablo Escobar ([pabloescobar-reborn](https://github.com/pabloescobar-reborn)) — AutoEQ headphone correction profiles
* swiitch-OFF-Lab ([swiitch-OFF-Lab](https://github.com/swiitch-OFF-Lab)) — Base tree ([hardware_dolby](https://github.com/swiitch-OFF-Lab/hardware_dolby))

## Getting Started

For dolby media codecs to work add this line in your media codecs config (should be in vendor partition) and make sure your device supports c2 codecs. :-

```bash
<Include href="media_codecs_dolby_audio.xml" />
```

To build, add the dolby effects in your device's audio effects config then inherit the dolby config by adding this in your device's makefile :-

```bash
$(call inherit-product, hardware/dolby/dolby.mk)
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
