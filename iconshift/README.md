# IconShift — Phase 1 proof of concept

IconShift will let you apply icons from installed icon packs to apps on the **stock HyperOS
launcher**. It needs no root, no third-party launcher and no pinned shortcuts.

This folder is **Phase 1 only**: a small probe app that tests whether a launcher icon on HyperOS 3
can really be replaced, and how. The full app (icon pack reader, browser, database, UI) starts
once a physical-device run shows a mechanism passing every check below.

> Android has no API for one app to change another app's launcher icon. The POC tries real
> mechanisms that use shell-level access (via Shizuku) and reports what actually happened.
> It never falls back to a shortcut and never reports success without verification.

## Layout

| Path | What |
|------|------|
| `core/` | Pure Kotlin, no Android dependencies. `IconApplyEngine` contract, `ApplyPipeline` (the only place a result becomes "Verified"), `ApplyEngineRegistry`, the MIUI theme `icons`/`.mtz` packager, and the `HyperOsThemeApplyEngine` logic. Unit-tested. |
| `app/` | Android POC (Kotlin, Compose M3, minSdk 30). Shizuku user service, device/ThemeManager probes, the `FabricatedOverlayApplyEngine`, and the one-screen POC UI. |
| `docs/HYPEROS_RESEARCH.md` | The candidate mechanisms, what each is expected to do, and a results table to fill in from device reports. |

## Build

CI (`.github/workflows/iconshift-android.yml`) runs `:core:test` and `:app:assembleDebug` on every
push. It publishes the APK in two places:

- **Direct download:** open
  <https://github.com/Kamal142-D/fitness-rpg/releases/download/iconshift-poc/iconshift-poc.apk>
  on the phone. This is a rolling *pre-release* that always holds the latest build. It is
  deliberately a pre-release so the Fitness RPG in-app updater, which reads `/releases/latest`,
  never picks it up.
- **Fallback:** the `iconshift-poc-debug-apk` artifact on the workflow run. It needs a GitHub
  login and downloads as a zip.

Locally, with an Android SDK:

```bash
cd iconshift
./gradlew :core:test :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Device test protocol (HyperOS 3)

1. **Shizuku.** Install Shizuku from Google Play or its GitHub releases, and start it with
   *Wireless debugging*. Shizuku's in-app guide covers this; no root and no PC needed.
2. Install the POC APK and open **IconShift POC**. Tap **Grant access** when Shizuku asks.
3. Check the **Device** card. It shows the HyperOS version, launcher, and whether ThemeManager
   exposes an apply entry point.
4. **Target app.** Pick an app and put its icon **in the dock** as well as on the home screen.
   Test two kinds of app:
   - one the current theme already styles (for example WhatsApp on the default theme)
   - one it doesn't (an app that normally gets a frame)
5. **Replacement icon.** Tap **From icon pack**, choose one of your installed packs, then tap an
   icon. The pack's suggestions for the target app are listed first, and the search box works
   across all icons. Long-press an icon to preview it. **Test icon** and **Pick image** still work
   too.
6. For **each engine card**:
   1. Tap **Apply** and watch the stages: Preparing → Applying → Refreshing launcher → Verifying.
      "VERIFIED" only means the system-level check passed.
   2. Go to the home screen and look. Then record Pass/Fail for: home screen, dock, no white
      frame, opens the real app, no duplicate icon, app still works.
   3. **Reboot**, reopen the POC and record "survives reboot". Tap **Verify** again.
   4. Tap **Restore**, check the launcher, and record "restore".
7. Tap **Diagnostics → Theme icons / Overlays / ThemeManager**. This adds the raw system state to
   the log.
8. Tap **Export report** and send the text back. It contains everything needed to pick the V1
   Apply Engine, or to adapt an engine to what HyperOS 3 actually exposes.

### Safety notes

- Neither engine modifies, re-signs or patches any installed APK.
- The HyperOS engine backs up the target app's original theme entries before changing them.
  **Restore** puts those entries back.
- The overlay engine's **Restore** removes the overlay it added. You can also run
  `adb shell cmd overlay list <package>` to see it.
- If an engine leaves things in a bad state, re-applying any theme in Xiaomi *Themes* resets the
  icons component.
