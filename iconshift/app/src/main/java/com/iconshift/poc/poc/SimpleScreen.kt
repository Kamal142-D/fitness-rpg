package com.iconshift.poc.poc

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iconshift.core.applyengine.ApplyStage
import com.iconshift.poc.R
import com.iconshift.poc.shizuku.ShizukuGate

/** The default screen: set up once, pick an app, pick an icon, tap Apply. Technical details live in Advanced. */
@Composable
fun SimpleScreen(
    state: PocState,
    vm: PocViewModel,
    padding: PaddingValues,
    onPickApp: () -> Unit,
    onPickImage: () -> Unit,
) {
    val context = LocalContext.current
    val engine = state.engines.firstOrNull { it.id == state.automaticEngineId }
    val busy = engine?.busy == true
    val shizukuReady = state.shizuku == ShizukuGate.Status.Ready
    val checked = state.engines.isNotEmpty()
    // Only ask for Shizuku when no method works without it (e.g. ThemeManager's apply screen is locked).
    val needsSetup = checked && engine == null && !shizukuReady

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (needsSetup) SetupCard(state.shizuku, vm, context)

        StepCard(stringResource(R.string.simple_step_app), onClick = onPickApp) {
            BigIcon(state.target?.icon, 64)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.target?.label ?: stringResource(R.string.simple_choose_app),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(stringResource(R.string.simple_change), color = MaterialTheme.colorScheme.primary)
        }

        StepCard(stringResource(R.string.simple_step_icon), onClick = vm::openIconPacks) {
            BigIcon(state.iconPreview, 64)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.icon?.origin?.startsWith("pack:") == true) state.icon.label
                    else stringResource(R.string.simple_choose_icon),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(stringResource(R.string.simple_change), color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = vm::useGeneratedIcon) { Text(stringResource(R.string.simple_test_icon)) }
            TextButton(onClick = onPickImage) { Text(stringResource(R.string.simple_gallery)) }
        }

        Button(
            onClick = vm::applyAutomatic,
            enabled = engine != null && !busy && state.target != null && state.icon != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.simple_apply), style = MaterialTheme.typography.titleMedium) }
        OutlinedButton(
            onClick = vm::restoreAutomatic,
            enabled = engine != null && !busy && state.target != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.simple_restore)) }

        if (checked && engine == null && shizukuReady) {
            Text(stringResource(R.string.simple_no_method), color = MaterialTheme.colorScheme.error)
        }
        if (engine != null) StatusArea(engine, vm, context)
    }
}

@Composable
private fun SetupCard(status: ShizukuGate.Status, vm: PocViewModel, context: Context) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.simple_setup_title), style = MaterialTheme.typography.titleMedium)
            when (status) {
                ShizukuGate.Status.NotInstalled -> {
                    Text(stringResource(R.string.simple_setup_install_body))
                    Button(onClick = { openShizukuStore(context) }) { Text(stringResource(R.string.simple_setup_get)) }
                }
                ShizukuGate.Status.NotRunning -> {
                    Text(stringResource(R.string.simple_setup_start_body))
                    Button(onClick = { openShizuku(context) }) { Text(stringResource(R.string.simple_setup_open)) }
                }
                ShizukuGate.Status.PermissionNeeded -> {
                    Text(stringResource(R.string.simple_setup_allow_body))
                    Button(onClick = vm::requestShizukuPermission) { Text(stringResource(R.string.simple_setup_allow)) }
                }
                ShizukuGate.Status.Ready -> Unit
            }
        }
    }
}

@Composable
private fun StatusArea(engine: EngineUi, vm: PocViewModel, context: Context) {
    if (engine.busy) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(stageLabel(engine.stage))
        }
        return
    }
    val outcome = engine.lastOutcome ?: return
    val reason = engine.lastReason.orEmpty()
    val message = when (outcome) {
        Outcome.Verified, Outcome.Unverified -> when {
            engine.lastAction == Action.Restore -> stringResource(R.string.simple_result_restored)
            outcome == Outcome.Verified -> stringResource(R.string.simple_result_done)
            else -> stringResource(R.string.simple_result_unverified)
        }
        Outcome.NeedsSetup -> stringResource(R.string.simple_result_setup)
        Outcome.Unsupported -> stringResource(R.string.simple_result_unsupported, reason)
        Outcome.Failed -> stringResource(R.string.simple_result_failed, reason)
    }
    val good = outcome == Outcome.Verified || outcome == Outcome.Unverified
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (good) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, style = MaterialTheme.typography.titleMedium)
            if (good && engine.lastAction == Action.Apply) {
                val answer = engine.checks[Check.HomeScreen]
                if (answer == null) {
                    Text(stringResource(R.string.simple_ask_worked))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.setCheck(engine.id, Check.HomeScreen, true) }) { Text(stringResource(R.string.simple_yes)) }
                        OutlinedButton(onClick = { vm.setCheck(engine.id, Check.HomeScreen, false) }) { Text(stringResource(R.string.simple_no)) }
                    }
                } else {
                    Text(stringResource(R.string.simple_thanks))
                }
            }
            OutlinedButton(onClick = { shareReport(context, vm) }) { Text(stringResource(R.string.simple_send_report)) }
        }
    }
}

@Composable
private fun StepCard(title: String, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Card(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { content() }
        }
    }
}

@Composable
private fun BigIcon(bitmap: ImageBitmap?, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(sizeDp.dp))
    }
}

@Composable
private fun stageLabel(stage: ApplyStage?): String = stringResource(
    when (stage) {
        ApplyStage.Applying -> R.string.simple_stage_applying
        ApplyStage.RefreshingLauncher -> R.string.simple_stage_refreshing
        ApplyStage.Verifying -> R.string.simple_stage_verifying
        ApplyStage.Preparing, null -> R.string.simple_stage_preparing
    },
)

private const val SHIZUKU_PACKAGE = ShizukuGate.SHIZUKU_PACKAGE

private fun openShizuku(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
    if (launch != null) context.startActivity(launch) else openShizukuStore(context)
}

private fun openShizukuStore(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PACKAGE"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE"))
    runCatching { context.startActivity(market) }.onFailure { runCatching { context.startActivity(web) } }
}

fun shareReport(context: Context, vm: PocViewModel) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "IconShift POC report")
        .putExtra(Intent.EXTRA_TEXT, vm.buildReport())
    context.startActivity(Intent.createChooser(send, "Send report"))
}
