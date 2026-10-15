package com.endroid.latch.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.endroid.latch.BuildConfig
import com.endroid.latch.data.ActionType
import com.endroid.latch.data.RingerModeKind
import com.endroid.latch.data.Rule
import com.endroid.latch.data.RuleAction
import com.endroid.latch.data.RuleStore
import com.endroid.latch.data.SettingsStore
import com.endroid.latch.data.TriggerType
import com.endroid.latch.engine.AppWatchService
import com.endroid.latch.engine.Elevated
import com.endroid.latch.engine.RuleEngine
import java.util.UUID

private sealed class Screen {
    data object List : Screen()
    data class Edit(val rule: Rule?) : Screen()
    data object Settings : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatchApp() {
    val context = LocalContext.current
    val store = remember { RuleStore(context) }
    var rules by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    var exportText by remember { mutableStateOf<String?>(null) }

    fun reload() {
        rules = store.load()
        AppWatchService.refresh(context)
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        RuleEngine.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    when (val s = screen) {
        Screen.List -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Latch") },
                    actions = {
                        IconButton(onClick = { screen = Screen.Settings }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { screen = Screen.Edit(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            },
        ) { padding ->
            if (rules.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No rules yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Build local if → then automations. Multi-step actions, Wi‑Fi, Bluetooth, apps, and optional root shell.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { screen = Screen.Edit(null) }) { Text("Create rule") }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(padding),
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onToggle = { store.setEnabled(rule.id, it); reload() },
                            onClick = { screen = Screen.Edit(rule) },
                            onDelete = { store.delete(rule.id); reload() },
                        )
                    }
                }
            }
        }
        is Screen.Edit -> EditRuleScreen(
            initial = s.rule,
            onBack = { screen = Screen.List },
            onSave = { store.upsert(it); reload(); screen = Screen.List },
        )
        Screen.Settings -> SettingsScreen(
            onBack = { screen = Screen.List },
            onExport = { exportText = store.exportJson() },
        )
    }

    exportText?.let { text ->
        AlertDialog(
            onDismissRequest = { exportText = null },
            title = { Text("Export JSON") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            },
            confirmButton = { TextButton(onClick = { exportText = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text(rule.triggerSummary(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (rule.actions.isNotEmpty()) {
                    Text(
                        rule.actions.joinToString(" → ") { it.summary() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (rule.cooldownSec > 0) {
                    Text("Cooldown ${rule.cooldownSec}s", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRuleScreen(
    initial: Rule?,
    onBack: () -> Unit,
    onSave: (Rule) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var trigger by remember { mutableStateOf(initial?.trigger ?: TriggerType.BATTERY_BELOW) }
    var value by remember { mutableIntStateOf(initial?.triggerValue ?: 20) }
    var value2 by remember { mutableIntStateOf(initial?.triggerValue2 ?: 0) }
    var triggerParam by remember { mutableStateOf(initial?.triggerParam ?: "") }
    var cooldown by remember { mutableIntStateOf(initial?.cooldownSec ?: 0) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var actions by remember {
        mutableStateOf(
            initial?.actions?.ifEmpty { listOf(RuleAction(ActionType.NOTIFY)) }
                ?: listOf(RuleAction(ActionType.NOTIFY)),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "New rule" else "Edit rule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            EnumDropdown("When", TriggerType.entries, trigger) { trigger = it }

            when (trigger) {
                TriggerType.BATTERY_BELOW, TriggerType.BATTERY_ABOVE ->
                    IntField("Battery %", value) { value = it.coerceIn(1, 100) }
                TriggerType.TIME_DAILY -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField("Hour", value, Modifier.weight(1f)) { value = it.coerceIn(0, 23) }
                    IntField("Minute", value2, Modifier.weight(1f)) { value2 = it.coerceIn(0, 59) }
                }
                TriggerType.WIFI_SSID ->
                    OutlinedTextField(triggerParam, { triggerParam = it },
                        label = { Text("SSID (exact)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                TriggerType.APP_OPENED ->
                    OutlinedTextField(triggerParam, { triggerParam = it },
                        label = { Text("App package name") },
                        placeholder = { Text("com.example.app") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                TriggerType.CUSTOM_BROADCAST ->
                    OutlinedTextField(triggerParam, { triggerParam = it },
                        label = { Text("Broadcast action") },
                        placeholder = { Text("com.myapp.EVENT") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                else -> Unit
            }

            Text("Actions (run in order)", style = MaterialTheme.typography.titleSmall)
            actions.forEachIndexed { index, action ->
                ActionEditor(
                    index = index,
                    action = action,
                    onChange = { a ->
                        actions = actions.toMutableList().also { it[index] = a }
                    },
                    onRemove = {
                        if (actions.size > 1) {
                            actions = actions.toMutableList().also { it.removeAt(index) }
                        }
                    },
                )
            }
            OutlinedButton(
                onClick = { actions = actions + RuleAction(ActionType.TOAST, text = "Next step") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add action") }

            IntField("Cooldown seconds (0 = none)", cooldown) { cooldown = it.coerceAtLeast(0) }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val n = name.ifBlank { trigger.name.lowercase().replace('_', ' ') }
                    onSave(
                        Rule(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = n,
                            enabled = initial?.enabled ?: true,
                            trigger = trigger,
                            triggerValue = value,
                            triggerValue2 = value2,
                            triggerParam = triggerParam,
                            actions = actions,
                            cooldownSec = cooldown,
                            notes = notes,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save rule") }
        }
    }
}

@Composable
private fun ActionEditor(
    index: Int,
    action: RuleAction,
    onChange: (RuleAction) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step ${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            EnumDropdown("Action", ActionType.entries, action.type) {
                onChange(action.copy(type = it))
            }
            when (action.type) {
                ActionType.NOTIFY -> {
                    OutlinedTextField(action.title, { onChange(action.copy(title = it)) },
                        label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(action.text, { onChange(action.copy(text = it)) },
                        label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
                }
                ActionType.TOAST ->
                    OutlinedTextField(action.text, { onChange(action.copy(text = it)) },
                        label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                ActionType.RINGER_MODE ->
                    EnumDropdown("Mode", RingerModeKind.entries, action.ringerMode) {
                        onChange(action.copy(ringerMode = it))
                    }
                ActionType.OPEN_APP ->
                    OutlinedTextField(action.param, { onChange(action.copy(param = it)) },
                        label = { Text("Package name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ActionType.OPEN_URL ->
                    OutlinedTextField(action.param, { onChange(action.copy(param = it)) },
                        label = { Text("URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ActionType.SET_BRIGHTNESS ->
                    OutlinedTextField(action.param, { onChange(action.copy(param = it)) },
                        label = { Text("Brightness 1–100") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ActionType.SHELL ->
                    OutlinedTextField(action.param, { onChange(action.copy(param = it)) },
                        label = { Text("Shell command") },
                        placeholder = { Text("cmd (uses su if elevated ON)") },
                        modifier = Modifier.fillMaxWidth())
                ActionType.WAIT_MS ->
                    OutlinedTextField(action.param, { onChange(action.copy(param = it)) },
                        label = { Text("Wait milliseconds") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ActionType.PLUGIN_BROADCAST -> {
                    OutlinedTextField(action.param, { onChange(action.copy(param = it)) },
                        label = { Text("Broadcast action") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(action.text, { onChange(action.copy(text = it)) },
                        label = { Text("Extra text") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(onBack: () -> Unit, onExport: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    var elevated by remember { mutableStateOf(settings.elevatedShell) }
    var plugin by remember { mutableStateOf(settings.pluginApiEnabled) }
    val hasSu = remember { Elevated.hasSu() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Latch ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
            Text("On-device automation. No account, no cloud, no trackers.")

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Elevated shell (root / su)")
                    Text(
                        if (hasSu) "su detected — shell actions can run as root"
                        else "su not found — shell runs as app user",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = elevated, onCheckedChange = {
                    elevated = it
                    settings.elevatedShell = it
                })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Plugin API")
                    Text(
                        "Allow other apps to fire triggers / run rules via broadcast",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = plugin, onCheckedChange = {
                    plugin = it
                    settings.pluginApiEnabled = it
                })
            }

            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }, modifier = Modifier.fillMaxWidth()) { Text("Usage access (app-opened triggers)") }

            OutlinedButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        },
                    )
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Write settings (brightness)") }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }, modifier = Modifier.fillMaxWidth()) { Text("Exact alarms (daily time)") }
            }

            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export rules JSON") }

            Text(
                "Plugin intents:\n" +
                    "com.endroid.latch.api.FIRE_TRIGGER (extra trigger=…)\n" +
                    "com.endroid.latch.api.RUN_RULE (extra rule_id=…)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("MIT · Project Endroid", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun IntField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onChange: (Int) -> Unit,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toIntOrNull() ?: value) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name.lowercase().replace('_', ' '),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.name.lowercase().replace('_', ' ')) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}
