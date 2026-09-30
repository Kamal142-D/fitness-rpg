# HyperOS 3 icon replacement: mechanisms and findings

Status: **unconfirmed**. Nothing below counts as supported until a physical HyperOS 3 device report
confirms it. Fill in the results table from exported POC reports.

## Constraints (from the spec)

The mechanism must not need root, a third-party launcher, a pinned shortcut or widget, or a manual
theme switch each time. It must not add a white frame. It must work in the dock, launch the real
app, survive reboot, and be restorable.

## How the HyperOS launcher picks an icon

`com.miui.home` loads icons through MIUI's theme layer (`IconCustomizer`), in this order:

1. If the **applied theme's `icons` component** has an entry for the package
   (`res/drawable-<dpi>/<package>.png`, or a per-app layered/fancy folder), that entry is used
   as-is.
2. Otherwise the app's own icon (from its resources) is loaded and then **masked and framed** to
   match the theme. This is where the "white border" around unthemed icons and pinned shortcuts
   comes from.

So a frameless custom icon has to come from step 1, or come from step 2 in a form HyperOS doesn't
frame. Pinned shortcuts go through step 2 plus shortcut badging, which is why they are excluded.

## Candidate mechanisms

### A0. HyperOS theme icons without Shizuku (`hyperos-theme-direct`, tried first)

This uses the same theme logic as A, but only normal app APIs:
- It reads `/data/system/theme/icons` directly, since theme files are world-readable.
- It falls back to `/system/media/theme/default/icons` as the base when no icons component is applied.
- It writes the `.mtz` to `Download/IconShift/` through MediaStore, which needs no permission.
- It opens ThemeManager's apply activity with a normal `startActivity`. The file path goes in `theme_file_path`, plus a read-granted content URI.

**When it works:** only if the apply activity is exported **without a permission**. The probe records this, including any permission inherited from the application. If no theme icons are readable at all, it refuses rather than reset other apps' icons.

**If it works:** no root, no Shizuku and no ADB are needed, and nothing needs to stay running in the background.

### A. HyperOS theme icons (`HyperOsThemeApplyEngine`, primary)

- **What it does:** rewrites the applied `icons` component so it holds our PNG for the target
  package. Every other entry is kept, so other apps keep their themed icons. It then asks
  ThemeManager to apply an icons-only `.mtz`.
- **Access:** reading `/data/system/theme/icons` (world-readable) and writing the `.mtz` to
  `/sdcard/Download/IconShift/` happen through the Shizuku shell. Applying uses ThemeManager's
  exported designer/test entry point: `am start -n com.android.thememanager/.ApplyThemeForScreenshot
  -e theme_file_path <mtz> -e api_called_from test`.
- **Verification:** the applied `icons` file must contain our PNG (SHA-256 match) under the
  target's package. Restore is verified by the entries matching the saved original.
- **Expected if it works:** dock and home screen both change (the launcher reads theme icons
  everywhere), there is no frame, the icon survives reboot (theme files persist), and the real
  app launches (launcher entries are untouched).
- **Risks and open questions:**
  - HyperOS 3 may have removed, renamed, or permission-locked the apply activity. The POC lists
    every exported ThemeManager activity with "apply", "import", "local" or "mtz" in its name so a
    replacement can be wired in.
  - The icons file path may have moved. The diagnostics list `/data/system/theme/`.
  - ThemeManager may reject a `description.xml` whose `uiVersion` doesn't match. The engine writes
    the device's `ro.miui.ui.version.code`.
  - Applying an icons-only theme may reset other theme components, or mark the theme as
    "mixed/custom".
  - Theme "fancy/layered" icons: the engine drops all of the target's entries so the flat PNG
    wins. Check that the launcher then uses the flat PNG.

### B. App resource overlay (`FabricatedOverlayApplyEngine`, secondary, Android 14+)

- **What it does:** registers a shell-owned fabricated overlay (`com.android.shell:iconshift_<pkg>`)
  targeting the app. It points the app's launcher-icon resources (for example
  `mipmap/ic_launcher` and `_round`) at our PNG, using `FabricatedOverlay.Builder.setResourceValue(name,
  ParcelFileDescriptor, config)`. The target APK is untouched, and removing the overlay restores
  the original.
- **Access:** `IOverlayManager.commit()` from the Shizuku user service. Shell holds
  `CHANGE_OVERLAY_PACKAGES`, and the shell package is a system package, so the overlay should pass
  idmap policy checks for targets that don't declare `<overlayable>`. `cmd overlay fabricate` is
  root-only, which is why the API is used.
- **Verification:** `cmd overlay list <pkg>` shows `[x]` for our overlay, **and** the icon resource
  renders differently from the recorded original.
- **Risks and open questions:**
  - For apps the theme covers, step 1 above wins, so the overlay may be invisible on the launcher.
  - For unthemed apps, HyperOS may still mask or frame the result.
  - idmap2 may refuse file descriptors from the shell domain (SELinux). The service tries a temp
    file first, then pipes, and reports both errors.
  - A PNG replacing an adaptive-icon XML resource may be handled differently by the launcher.

### Rejected or deferred

| Mechanism | Why |
|-----------|-----|
| Pinned shortcuts (`ShortcutManager`) | Explicitly excluded: duplicate icon plus HyperOS frame/badge. |
| Launcher database edits (`com.miui.home` private DB) | Not readable or writable by shell; would need root. |
| Patching or re-signing target APKs | Forbidden by the spec and Play policy. |
| RRO overlay APKs installed normally | Android 10+ idmap policy rejects unsigned third-party overlays on targets without `<overlayable>`. |
| Accessibility or automation of the Themes app | Fragile and slow, and still a "manual theme workflow" in disguise. Only a last resort. |

## Results (fill from device reports)

| Date | Device / HyperOS build | Engine | Target themed? | Home | Dock | No frame | Launches app | Reboot | Restore | Notes |
|------|------------------------|--------|----------------|------|------|----------|--------------|--------|---------|-------|
| 2026-09-30 | Xiaomi klimt (2506BPN68G), HyperOS OS3.0.333.0.XOSMIXM, Android 17, ThemeManager 3.0.5.14-global | A0 (no Shizuku) | WhatsApp, stock theme (no applied `icons`; `.runtime/` unreadable) | – | – | – | – | – | – | `ApplyThemeForScreenshot` is exported with no permission, but `startActivity` fails with Xiaomi code **-50** (app-launch interception). Next: retry without the data URI; otherwise use A (Shizuku `am start`). |

## Decision

To be made after the first reports: pick the V1 `HyperOSApplyEngine` implementation, or adapt the
mechanism based on what the probes show.
