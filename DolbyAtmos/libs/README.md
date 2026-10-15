# Vendored prebuilts for the liquid-glass navbar

- `miuix-blur-0.9.1.jar` — `classes.jar` extracted from
  `top.yukonga.miuix.kmp:miuix-blur-android:0.9.1`
  (https://github.com/compose-miuix-ui/miuix, Apache 2.0).
  Backdrop-blur engine behind the navbar's liquid-glass path.

Version pin note: 0.9.1 is the newest miuix-blur built with Kotlin
2.3.x metadata, which the platform kotlinc 2.2.0 in this tree can
read (it accepts metadata up to 2.3.0; 0.9.2+ needs Kotlin 2.4).

Jars (not AARs) are vendored deliberately: the artifact carries no
resources, and using `java_import` avoids a manifest merge that would
raise the app's minSdk. The glass route is still runtime-gated to
API 33+; older releases use the snapshot-based fallback in
`RealBlurBackdrop`.

Kotlin support files live in `src/org/dolbyatmos/ui/glass/`:
`liquid/` (CombinedBackdrop, InnerShadow, Lens, Vibrancy), adapted
from OriginSU (`~/OriginSU/manager/.../ui/component/`, Apache 2.0).
