package com.iconshift.core.miui

import com.iconshift.core.FakeAccess
import com.iconshift.core.FakeShell
import com.iconshift.core.applyengine.AppTarget
import com.iconshift.core.applyengine.ApplyPipeline
import com.iconshift.core.applyengine.ApplyResult
import com.iconshift.core.applyengine.ApplyStage
import com.iconshift.core.applyengine.IconSource
import com.iconshift.core.applyengine.InMemoryEngineStateStore
import com.iconshift.core.applyengine.Requirement
import com.iconshift.core.applyengine.SupportLevel
import com.iconshift.core.fakePng
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HyperOsThemeApplyEngineTest {

    private val iconsPath = "/data/system/theme/icons"
    private val target = AppTarget("com.whatsapp", null, "WhatsApp")
    private val icon = IconSource(fakePng("custom"), "test", "generated")
    private val env = HyperOsThemeEnvironment(
        isMiuiFamily = true,
        osDescription = "HyperOS OS3.0",
        uiVersionCode = "816",
        applyComponent = "com.android.thememanager/com.android.thememanager.ApplyThemeForScreenshot",
    )

    private fun engine(shell: FakeShell, access: FakeAccess = FakeAccess(shell), e: HyperOsThemeEnvironment = env) =
        HyperOsThemeApplyEngine(ShellThemeHost(access), { e }, InMemoryEngineStateStore(), pollIntervalMs = 10, pollAttempts = 5, clock = { 42L })

    private fun themedShell() = FakeShell(iconsPath).apply {
        put(
            iconsPath,
            MiuiIconsZip.write(
                linkedMapOf(
                    "res/drawable-xxhdpi/com.whatsapp.png" to fakePng("wa-theme"),
                    "res/drawable-xxhdpi/com.android.chrome.png" to fakePng("chrome"),
                ),
            ),
        )
    }

    @Test
    fun `apply then restore round-trips and both are verified`() = runTest {
        val shell = themedShell()
        val engine = engine(shell)
        val stages = mutableListOf<ApplyStage>()

        val applied = ApplyPipeline.apply(engine, target, icon) { stages += it }
        assertTrue(applied.toString(), applied is ApplyResult.Verified)
        assertEquals(ApplyStage.entries.toList(), stages)
        val afterApply = MiuiIconsZip.read(shell.files.getValue(iconsPath))
        assertArrayEquals(icon.pngBytes, afterApply["res/drawable-xxhdpi/com.whatsapp.png"])
        assertArrayEquals(fakePng("chrome"), afterApply["res/drawable-xxhdpi/com.android.chrome.png"])
        assertTrue(shell.files.containsKey("/sdcard/Download/IconShift/iconshift_42.mtz"))

        val restored = ApplyPipeline.restore(engine, target)
        assertTrue(restored.toString(), restored is ApplyResult.Verified)
        val afterRestore = MiuiIconsZip.read(shell.files.getValue(iconsPath))
        assertArrayEquals(fakePng("wa-theme"), afterRestore["res/drawable-xxhdpi/com.whatsapp.png"])
    }

    @Test
    fun `applying twice keeps the original theme icon as the backup`() = runTest {
        val shell = themedShell()
        val engine = engine(shell)
        ApplyPipeline.apply(engine, target, icon)
        ApplyPipeline.apply(engine, target, IconSource(fakePng("second"), "b", "generated"))
        ApplyPipeline.restore(engine, target)
        val after = MiuiIconsZip.read(shell.files.getValue(iconsPath))
        assertArrayEquals(fakePng("wa-theme"), after["res/drawable-xxhdpi/com.whatsapp.png"])
    }

    @Test
    fun `restoring an app that was never themed removes the override`() = runTest {
        val shell = FakeShell(iconsPath) // no icons component applied at all
        val engine = engine(shell)
        assertTrue(ApplyPipeline.apply(engine, target, icon) is ApplyResult.Verified)
        assertTrue(ApplyPipeline.restore(engine, target) is ApplyResult.Verified)
        val after = MiuiIconsZip.read(shell.files.getValue(iconsPath))
        assertFalse(after.keys.any { MiuiIconsZip.belongsTo(it, "com.whatsapp") })
    }

    @Test
    fun `ThemeManager refusal is a failure, never success`() = runTest {
        val shell = themedShell().apply { themeManagerWorks = false }
        val result = ApplyPipeline.apply(engine(shell), target, icon)
        assertTrue(result.toString(), result is ApplyResult.Failed)
    }

    @Test
    fun `icons file that never changes stays unverified`() = runTest {
        val shell = themedShell().apply { themeManagerIgnores = true }
        val result = ApplyPipeline.apply(engine(shell), target, icon)
        assertTrue(result.toString(), result is ApplyResult.AppliedUnverified)
        assertTrue((result as ApplyResult.AppliedUnverified).details.contains("did not change"))
    }

    @Test
    fun `missing Shizuku setup short-circuits to NeedsSetup`() = runTest {
        val shell = themedShell()
        val access = FakeAccess(shell, missing = listOf(Requirement.ShizukuRunning))
        val result = ApplyPipeline.apply(engine(shell, access), target, icon)
        assertEquals(ApplyResult.NeedsSetup(listOf(Requirement.ShizukuRunning), "Additional permission is required."), result)
        assertTrue(shell.commands.none { it.startsWith("am start") })
    }

    @Test
    fun `non-MIUI or no apply entry point is unsupported`() = runTest {
        val shell = themedShell()
        assertEquals(
            SupportLevel.Unsupported,
            engine(shell, e = env.copy(isMiuiFamily = false)).checkCompatibility().level,
        )
        val noEntry = engine(shell, e = env.copy(applyComponent = null))
        assertEquals(SupportLevel.Unsupported, noEntry.checkCompatibility().level)
        assertTrue(ApplyPipeline.apply(noEntry, target, icon) is ApplyResult.Unsupported)
    }

    @Test
    fun `invalid package names are rejected before touching the shell`() = runTest {
        val shell = themedShell()
        val result = ApplyPipeline.apply(engine(shell), AppTarget("x'; reboot; '", null, "bad"), icon)
        assertTrue(result is ApplyResult.Failed)
        assertTrue(shell.commands.none { it.startsWith("am start") })
        assertEquals(setOf(iconsPath), shell.files.keys) // nothing written
    }

    @Test
    fun `direct mode is unsupported when the apply screen needs a permission`() = runTest {
        val shell = themedShell()
        val direct = HyperOsThemeApplyEngine(
            ShellThemeHost(FakeAccess(shell)),
            { env.copy(applyComponentPermission = "miui.permission.USE_INTERNAL_GENERAL_API") },
            InMemoryEngineStateStore(),
            direct = true,
        )
        assertEquals(HyperOsThemeApplyEngine.DIRECT_ID, direct.id)
        assertEquals(SupportLevel.Unsupported, direct.checkCompatibility().level)
        assertTrue(ApplyPipeline.apply(direct, target, icon) is ApplyResult.Unsupported)
    }

    @Test
    fun `unreadable theme icons downgrade to limited`() = runTest {
        val shell = FakeShell(iconsPath) // nothing at the icons path
        assertEquals(SupportLevel.Limited, engine(shell).checkCompatibility().level)
    }

    @Test
    fun `stock theme uses the system default icons as base so other apps keep theirs`() = runTest {
        val shell = FakeShell(iconsPath).apply {
            put(
                "/system/media/theme/default/icons",
                MiuiIconsZip.write(linkedMapOf("res/drawable-xxhdpi/com.android.chrome.png" to fakePng("chrome-default"))),
            )
        }
        assertTrue(ApplyPipeline.apply(engine(shell), target, icon) is ApplyResult.Verified)
        val applied = MiuiIconsZip.read(shell.files.getValue(iconsPath))
        assertArrayEquals(fakePng("chrome-default"), applied["res/drawable-xxhdpi/com.android.chrome.png"])
        assertArrayEquals(icon.pngBytes, applied["res/drawable-xxhdpi/com.whatsapp.png"])
    }

    @Test
    fun `direct mode refuses when no theme icons are readable`() = runTest {
        val shell = FakeShell(iconsPath)
        val direct = HyperOsThemeApplyEngine(ShellThemeHost(FakeAccess(shell)), { env }, InMemoryEngineStateStore(), direct = true)
        assertEquals(SupportLevel.Unsupported, direct.checkCompatibility().level)
    }

    @Test
    fun `a remembered block makes the direct engine unsupported without touching anything`() = runTest {
        val shell = themedShell()
        val direct = HyperOsThemeApplyEngine(
            ShellThemeHost(FakeAccess(shell)), { env }, InMemoryEngineStateStore(),
            direct = true, blockedReason = { "blocked earlier (code -50)" },
        )
        assertEquals(SupportLevel.Unsupported, direct.checkCompatibility().level)
        assertEquals(ApplyResult.Unsupported("blocked earlier (code -50)"), ApplyPipeline.apply(direct, target, icon))
        assertTrue(shell.commands.none { it.startsWith("am start") })
    }

    @Test
    fun `a refused launch in direct mode is remembered and reported as unsupported`() = runTest {
        val shell = themedShell().apply { themeManagerWorks = false }
        var remembered: String? = null
        val direct = HyperOsThemeApplyEngine(
            ShellThemeHost(FakeAccess(shell)), { env }, InMemoryEngineStateStore(),
            pollIntervalMs = 10, pollAttempts = 2, direct = true, onLaunchBlocked = { remembered = it },
        )
        val result = ApplyPipeline.apply(direct, target, icon)
        assertTrue(result.toString(), result is ApplyResult.Unsupported)
        assertTrue((result as ApplyResult.Unsupported).reason.startsWith(HyperOsThemeApplyEngine.BLOCKED_DIRECT_MESSAGE))
        assertTrue(remembered != null)
    }
}
