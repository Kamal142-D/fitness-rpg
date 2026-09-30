package com.iconshift.poc.poc

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iconshift.core.applyengine.ApplyStage
import com.iconshift.poc.R
import com.iconshift.poc.shizuku.ShizukuGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocScreen(vm: PocViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickingApp by remember { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = advanced) { advanced = false }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.usePickedImage(uri)
    }

    val launchImagePicker = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (advanced) "Advanced · test details" else stringResource(R.string.simple_title)) },
                actions = {
                    TextButton(onClick = { advanced = !advanced }) {
                        Text(stringResource(if (advanced) R.string.simple_back else R.string.simple_advanced))
                    }
                },
            )
        },
    ) { padding ->
        if (!advanced) {
            SimpleScreen(state, vm, padding, onPickApp = { pickingApp = true }, onPickImage = launchImagePicker)
        } else LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Section("Device") {
                    state.deviceLines.forEach { Mono(it) }
                    Spacer(Modifier.padding(4.dp))
                    state.themeLines.forEach { Mono(it) }
                    if (state.iconPackSummary.isNotBlank()) Mono(state.iconPackSummary)
                    TextButton(onClick = vm::rerunChecks) { Text("Re-run checks") }
                }
            }
            item {
                Section("Shizuku") {
                    Text(shizukuText(state.shizuku) + if (state.shizukuDetail.isNotBlank()) " — ${state.shizukuDetail}" else "")
                    if (state.shizuku == ShizukuGate.Status.PermissionNeeded) {
                        Button(onClick = vm::requestShizukuPermission) { Text("Grant access") }
                    }
                    if (state.shizuku != ShizukuGate.Status.Ready) {
                        Text(
                            "Both mechanisms need shell-level access. Install Shizuku, start it with Wireless debugging " +
                                "(no root needed), then come back.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Section("Target app") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBox(state.target?.icon, 56)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(state.target?.label ?: "None", fontWeight = FontWeight.SemiBold)
                            Text(state.target?.packageName ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { pickingApp = true }) { Text("Change") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBox(state.targetResourceIcon, 40)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Icon as loaded from the app's own resources (reflects overlays, not theme icons)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Section("Replacement icon") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBox(state.iconPreview, 72)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(state.icon?.label ?: "-")
                            Button(onClick = vm::openIconPacks) { Text("From icon pack") }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = vm::useGeneratedIcon) { Text("Test icon") }
                                OutlinedButton(onClick = launchImagePicker) { Text("Pick image") }
                            }
                        }
                    }
                }
            }
            item {
                ApplyCard(state, vm)
            }
            item {
                Text("Automatic engine: ${state.automaticEngine}", style = MaterialTheme.typography.bodyMedium)
            }
            items(state.engines, key = { it.id }) { engine ->
                EngineCard(engine, vm)
            }
            item {
                Section("Diagnostics") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = vm::inspectThemeIcons, enabled = !state.diagnosticsBusy) { Text("Theme icons") }
                        OutlinedButton(onClick = vm::dumpOverlays, enabled = !state.diagnosticsBusy) { Text("Overlays") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = vm::dumpThemeManager, enabled = !state.diagnosticsBusy) { Text("ThemeManager") }
                        OutlinedButton(onClick = vm::restartLauncher, enabled = !state.diagnosticsBusy) { Text("Restart launcher") }
                    }
                    Button(onClick = { shareReport(context, vm) }) { Text("Export report") }
                }
            }
            item {
                Section("Log") {
                    SelectionContainer {
                        Column { state.log.takeLast(80).forEach { Mono(it) } }
                    }
                }
            }
        }
    }

    if (state.picker.open) {
        IconPackPickerDialog(state.picker, state.target?.label, vm)
    }

    if (pickingApp) {
        AppPickerDialog(state.apps, onPick = { vm.selectTarget(it); pickingApp = false }, onDismiss = { pickingApp = false })
    }
}

/** The main action: target -> chosen icon, one tap to apply with the best usable engine. */
@Composable
private fun ApplyCard(state: PocState, vm: PocViewModel) {
    val engine = state.engines.firstOrNull { it.id == state.automaticEngineId }
    val busy = engine?.busy == true
    Section("Apply") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(state.target?.icon, 56)
            Text("  →  ", style = MaterialTheme.typography.titleLarge)
            IconBox(state.iconPreview, 56)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.target?.label ?: "Choose a target app", fontWeight = FontWeight.SemiBold)
                Text(state.icon?.label ?: "-", style = MaterialTheme.typography.bodySmall)
            }
        }
        when {
            state.shizuku != ShizukuGate.Status.Ready -> Text(
                "Start Shizuku and tap \"Grant access\" in the Shizuku card first.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            !state.enginesChecked -> Text("Checking what works on this phone…", style = MaterialTheme.typography.bodySmall)
            engine == null -> Text(
                "No working apply method on this device yet: ${state.automaticEngine}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            else -> Text("Method: ${engine.name}", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = vm::applyAutomatic,
                enabled = engine != null && !busy && state.target != null && state.icon != null,
            ) { Text("Apply icon") }
            TextButton(onClick = vm::restoreAutomatic, enabled = engine != null && !busy) { Text("Restore original") }
        }
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(engine?.stage?.let(::stageText) ?: "Working…")
            }
        }
        engine?.lastResult?.let { Mono(it) }
        Text(
            "Then check the home screen and dock, and record Pass/Fail in the method's card below. " +
                "To try a specific method, use the engine cards below.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun EngineCard(engine: EngineUi, vm: PocViewModel) {
    Section(engine.name) {
        engine.compatibility?.let { c ->
            Text("${c.level}", fontWeight = FontWeight.SemiBold)
            c.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            if (c.missingRequirements.isNotEmpty()) {
                Text("Needs: " + c.missingRequirements.joinToString { it.description }, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.apply(engine.id) }, enabled = !engine.busy) { Text("Apply") }
            OutlinedButton(onClick = { vm.verify(engine.id) }, enabled = !engine.busy) { Text("Verify") }
            OutlinedButton(onClick = { vm.restore(engine.id) }, enabled = !engine.busy) { Text("Restore") }
        }
        if (engine.busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(engine.stage?.let(::stageText) ?: "Working…")
            }
        }
        engine.lastResult?.let { Mono(it) }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text("Check on the launcher, then record:", style = MaterialTheme.typography.labelMedium)
        Check.entries.forEach { check ->
            CheckRow(check.label, engine.checks[check]) { vm.setCheck(engine.id, check, it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckRow(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        FilterChip(
            selected = value == true,
            onClick = { onChange(if (value == true) null else true) },
            label = { Text("Pass") },
        )
        Spacer(Modifier.width(6.dp))
        FilterChip(
            selected = value == false,
            onClick = { onChange(if (value == false) null else false) },
            label = { Text("Fail") },
        )
    }
}

@Composable
private fun AppPickerDialog(apps: List<AppEntry>, onPick: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Choose target app") },
        text = {
            Column {
                OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, label = { Text("Search") })
                val filtered = apps.filter {
                    query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
                }
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = { "${it.packageName}/${it.activityName}" }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBox(app.icon, 36)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(app.label)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun IconBox(bitmap: ImageBitmap?, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(sizeDp.dp))
    }
}

@Composable
private fun Mono(text: String) {
    Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
}

private fun shizukuText(s: ShizukuGate.Status) = when (s) {
    ShizukuGate.Status.NotInstalled -> "Not installed"
    ShizukuGate.Status.NotRunning -> "Installed but not running"
    ShizukuGate.Status.PermissionNeeded -> "Running — IconShift needs permission"
    ShizukuGate.Status.Ready -> "Ready"
}

private fun stageText(stage: ApplyStage) = when (stage) {
    ApplyStage.Preparing -> "Preparing"
    ApplyStage.Applying -> "Applying"
    ApplyStage.RefreshingLauncher -> "Refreshing launcher"
    ApplyStage.Verifying -> "Verifying"
}
