package com.iconshift.poc.poc

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iconshift.core.iconpack.IconEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full-screen picker: installed icon packs, then a searchable, lazily-loaded icon grid. */
@Composable
fun IconPackPickerDialog(picker: PickerState, targetLabel: String?, vm: PocViewModel) {
    val back = { if (picker.selected != null) vm.backToPacks() else vm.closeIconPacks() }
    Dialog(onDismissRequest = back, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = back) { Text(if (picker.selected != null) "‹ Packs" else "Close") }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        picker.selected?.info?.label ?: "Installed icon packs",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (picker.selected == null) {
                    PackList(picker, vm)
                } else {
                    IconGrid(picker, targetLabel, vm)
                }
            }
        }
    }
}

@Composable
private fun PackList(picker: PickerState, vm: PocViewModel) {
    when {
        picker.loadingPacks -> Loading("Looking for icon packs…")
        picker.packs.isEmpty() -> Text(
            "No icon packs detected.\n\nIconShift finds packs that support common launchers " +
                "(Nova, ADW, Apex, Go, Action…). If your pack is installed but missing here, " +
                "export the report so its package can be added.",
        )
        else -> LazyColumn {
            items(picker.packs, key = { it.info.packageName }) { pack ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = pack.error == null) { vm.selectPack(pack) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Thumb(pack.icon, 48)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pack.info.label, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                pack.error != null -> pack.error
                                pack.info.iconCount != null -> "%,d icons".format(pack.info.iconCount)
                                else -> "Counting icons…"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(pack.info.packageName, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconGrid(picker: PickerState, targetLabel: String?, vm: PocViewModel) {
    val pkg = picker.selected?.info?.packageName ?: return
    var preview by remember { mutableStateOf<IconEntry?>(null) }

    OutlinedTextField(
        value = picker.query,
        onValueChange = vm::searchIcons,
        singleLine = true,
        placeholder = { Text("Search icons…") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    picker.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (picker.loadingIndex) {
        Loading("Reading icon pack…")
        return
    }

    val showRecommended = picker.query.isBlank() && picker.recommended.isNotEmpty()
    LazyVerticalGrid(columns = GridCells.Adaptive(64.dp), modifier = Modifier.fillMaxSize()) {
        if (showRecommended) {
            item(key = "h-rec", span = { GridItemSpan(maxLineSpan) }) {
                Header("Recommended for ${targetLabel ?: "this app"}")
            }
            items(picker.recommended, key = { "r:" + it.drawableName }) { entry ->
                IconCell(pkg, entry, vm, onClick = { vm.useIconFromPack(entry) }, onLongClick = { preview = entry })
            }
        }
        item(key = "h-all", span = { GridItemSpan(maxLineSpan) }) {
            Header(if (picker.query.isBlank()) "All icons (%,d)".format(picker.total) else "${picker.results.size} results")
        }
        items(picker.results, key = { "a:" + it.drawableName }) { entry ->
            IconCell(pkg, entry, vm, onClick = { vm.useIconFromPack(entry) }, onLongClick = { preview = entry })
        }
    }

    preview?.let { entry ->
        AlertDialog(
            onDismissRequest = { preview = null },
            confirmButton = {
                Button(onClick = {
                    preview = null
                    vm.useIconFromPack(entry)
                }) { Text("Use icon") }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Cancel") } },
            title = { Text(entry.label) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val big = rememberThumbnail(pkg, entry.drawableName, 384, vm)
                    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        big?.let { Image(it, contentDescription = entry.label, modifier = Modifier.fillMaxSize()) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(entry.drawableName, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (entry.mappedComponents.isEmpty()) "Not mapped to any app"
                        else "Mapped to:\n" + entry.mappedComponents.take(5).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconCell(pkg: String, entry: IconEntry, vm: PocViewModel, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bmp = rememberThumbnail(pkg, entry.drawableName, 144, vm)
    Box(
        Modifier
            .aspectRatio(1f)
            .padding(6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        bmp?.let { Image(it, contentDescription = entry.label, modifier = Modifier.fillMaxSize()) }
    }
}

/** Decodes one icon off the main thread when its cell becomes visible (cached in the repository). */
@Composable
private fun rememberThumbnail(pkg: String, drawableName: String, sizePx: Int, vm: PocViewModel): ImageBitmap? {
    val bmp by produceState<ImageBitmap?>(null, pkg, drawableName, sizePx) {
        value = withContext(Dispatchers.IO) { vm.packThumbnail(pkg, drawableName, sizePx)?.asImageBitmap() }
    }
    return bmp
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun Loading(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text)
    }
}

@Composable
private fun Thumb(bitmap: ImageBitmap?, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(sizeDp.dp))
    }
}
