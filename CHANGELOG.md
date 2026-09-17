# Changelog

### 2026-09-17

- Add framework FX tab (bass boost, virtualizer, reverb, loudness) to dynamics processing
- Add transparent navbar style with retained live blur, rename Page Style to Customization
- Fix navbar blur stutter/delay with vsync-aligned captures and faster throttles
- Fix output switcher to reflect the live media route on the home card and picker
- Fix swipe-back to match the fade transitions (predictive-back system animation opt-out)
- Collapse dynamics bands into per-band expandable rows, equalizer page honors card settings
- Whole-app pill styling with PILL corners by default, spinning Dolby logo in decoders section
- Align home header padding with body content, drop reset toasts for profile resets

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
