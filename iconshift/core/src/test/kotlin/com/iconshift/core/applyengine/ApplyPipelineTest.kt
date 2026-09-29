package com.iconshift.core.applyengine

import com.iconshift.core.fakePng
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyPipelineTest {

    private val target = AppTarget("a.b", null, "A")
    private val icon = IconSource(fakePng("i"), "i", "generated")

    private class ScriptedEngine(
        val applyResult: ApplyResult,
        val verification: VerificationResult,
        val level: SupportLevel = SupportLevel.Experimental,
        override val id: String = "scripted",
    ) : IconApplyEngine {
        override val displayName = id
        override suspend fun checkCompatibility() = CompatibilityResult(id, level, emptyList())
        override suspend fun apply(target: AppTarget, icon: IconSource, onStage: (ApplyStage) -> Unit) = applyResult
        override suspend fun restore(target: AppTarget, onStage: (ApplyStage) -> Unit) = applyResult
        override suspend fun verify(target: AppTarget) = verification
    }

    @Test
    fun `engine self-reported success is not trusted without verification`() = runTest {
        val engine = ScriptedEngine(ApplyResult.Verified("m", "claims"), VerificationResult.Unknown("no access"))
        assertTrue(ApplyPipeline.apply(engine, target, icon) is ApplyResult.AppliedUnverified)
    }

    @Test
    fun `apply is verified only when custom icon is active`() = runTest {
        val ok = ScriptedEngine(ApplyResult.AppliedUnverified("m", "d"), VerificationResult.CustomIconActive("yes"))
        assertTrue(ApplyPipeline.apply(ok, target, icon) is ApplyResult.Verified)
        val wrong = ScriptedEngine(ApplyResult.AppliedUnverified("m", "d"), VerificationResult.OriginalIconActive("still original"))
        assertTrue(ApplyPipeline.apply(wrong, target, icon) is ApplyResult.AppliedUnverified)
    }

    @Test
    fun `restore is verified only when original icon is active`() = runTest {
        val ok = ScriptedEngine(ApplyResult.AppliedUnverified("m", "d"), VerificationResult.OriginalIconActive("orig"))
        assertTrue(ApplyPipeline.restore(ok, target) is ApplyResult.Verified)
        val wrong = ScriptedEngine(ApplyResult.AppliedUnverified("m", "d"), VerificationResult.CustomIconActive("custom"))
        assertTrue(ApplyPipeline.restore(wrong, target) is ApplyResult.AppliedUnverified)
    }

    @Test
    fun `engine exceptions become failures`() = runTest {
        val engine = object : IconApplyEngine by ScriptedEngine(ApplyResult.Failed("x"), VerificationResult.Unknown("x")) {
            override suspend fun apply(target: AppTarget, icon: IconSource, onStage: (ApplyStage) -> Unit): ApplyResult =
                error("boom")
        }
        val r = ApplyPipeline.apply(engine, target, icon)
        assertTrue(r is ApplyResult.Failed && r.reason.contains("boom"))
    }

    @Test
    fun `unsupported engine never succeeds`() = runTest {
        val r = ApplyPipeline.apply(UnsupportedApplyEngine("nope"), target, icon)
        assertEquals(ApplyResult.Unsupported("nope"), r)
    }

    @Test
    fun `registry picks best usable engine and explains when none`() = runTest {
        val limited = ScriptedEngine(ApplyResult.Failed("x"), VerificationResult.Unknown("x"), SupportLevel.Limited, "limited")
        val experimental = ScriptedEngine(ApplyResult.Failed("x"), VerificationResult.Unknown("x"), SupportLevel.Experimental, "exp")
        val unsupported = ScriptedEngine(ApplyResult.Failed("x"), VerificationResult.Unknown("x"), SupportLevel.Unsupported, "un")
        assertEquals("exp", ApplyEngineRegistry(listOf(limited, unsupported, experimental)).automatic().id)
        assertEquals("unsupported", ApplyEngineRegistry(listOf(unsupported)).automatic().id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `IconSource rejects non-PNG data`() {
        IconSource(byteArrayOf(1, 2, 3), "x", "generated")
    }
}
