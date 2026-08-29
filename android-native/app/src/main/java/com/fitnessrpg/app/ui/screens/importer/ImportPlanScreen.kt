package com.fitnessrpg.app.ui.screens.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.data.repo.ImportPreview
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AppTextField
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Import a shared LiftoffRank preset link into a Gate. */
@Composable
fun ImportPlanScreen(
    userId: String,
    initialUrl: String?,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(initialUrl.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<ImportPreview?>(null) }

    fun fetch() {
        if (url.isBlank() || loading) return
        loading = true; error = null; preview = null
        scope.launch {
            runCatching { ServiceLocator.importRepository.previewFromUrl(url) }
                .onSuccess { preview = it }
                .onFailure { error = friendlyDataError(it, it.message ?: "Couldn't read that link.") }
            loading = false
        }
    }

    // Auto-fetch when a link was shared into the app.
    LaunchedEffect(Unit) { if (!initialUrl.isNullOrBlank()) fetch() }

    ScreenScaffold {
        ScreenHeader(
            title = "Import a plan",
            subtitle = "Turn a shared LiftoffRank link into a Gate.",
            action = { AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST) },
        )

        AppTextField(
            value = url,
            onValueChange = { url = it },
            label = "LiftoffRank link",
            placeholder = "https://liftoffrank.com/preset/…",
            keyboardType = KeyboardType.Uri,
        )
        AppButton(
            if (loading) "Reading plan…" else "Fetch plan",
            onClick = { fetch() },
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.SECONDARY,
            loading = loading,
        )

        error?.let { AppCard { AppText(it, tone = TextTone.DANGER) } }

        preview?.let { p ->
            AppCard {
                AppText(p.planName, variant = TextVariant.TITLE)
                AppText(
                    "${p.matched.size} exercise${if (p.matched.size == 1) "" else "s"} matched" +
                        if (p.unmatched.isEmpty()) "" else "  ·  ${p.unmatched.size} skipped",
                    variant = TextVariant.CAPTION,
                    tone = TextTone.SECONDARY,
                )
            }

            AppCard(modifier = Modifier.fillMaxWidth(), padding = Spacing.none) {
                p.matched.forEachIndexed { index, ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppText((index + 1).toString().padStart(2, '0'), variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
                        Column(Modifier.weight(1f)) {
                            AppText(ex.catalogName, variant = TextVariant.LABEL)
                            if (!ex.importedName.equals(ex.catalogName, ignoreCase = true)) {
                                AppText("from “${ex.importedName}”", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
                            }
                        }
                        AppText(
                            "${ex.sets} × ${if (ex.repsMin == ex.repsMax) "${ex.repsMax}" else "${ex.repsMin}–${ex.repsMax}"}",
                            variant = TextVariant.CAPTION,
                            tone = TextTone.SECONDARY,
                            mono = true,
                        )
                    }
                    if (index != p.matched.lastIndex) HorizontalDivider(color = Palette.Hairline)
                }
            }

            if (p.unmatched.isNotEmpty()) {
                AppCard {
                    AppText("NOT IN YOUR CATALOG", variant = TextVariant.CAPTION, tone = TextTone.ACCENT)
                    AppText(
                        "These couldn't be matched and were skipped: ${p.unmatched.joinToString(", ")}.",
                        variant = TextVariant.CAPTION,
                        tone = TextTone.SECONDARY,
                    )
                }
            }

            AppButton(
                if (creating) "Creating Gate…" else "Create Gate",
                onClick = {
                    if (creating) return@AppButton
                    creating = true; error = null
                    scope.launch {
                        runCatching { ServiceLocator.importRepository.createGate(userId, p) }
                            .onSuccess { onCreated(it) }
                            .onFailure { error = friendlyDataError(it, it.message ?: "Couldn't create the Gate."); creating = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = p.matched.isNotEmpty(),
                loading = creating,
            )
        }
    }
}
