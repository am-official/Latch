package com.endroid.latch.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import com.endroid.latch.data.TriggerType
import com.endroid.latch.engine.RuleEngine

private sealed class Screen {
    data object List : Screen()
    data class Edit(val rule: Rule?) : Screen()
    data object About : Screen()
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
    }

    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or not — engine handles SecurityException */ }

    LaunchedEffect(Unit) {
        RuleEngine.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!ok) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    when (val s = screen) {
        Screen.List -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Latch") },
                    actions = {
                        IconButton(onClick = { screen = Screen.About }) {
                            Icon(Icons.Default.Info, contentDescription = "About")
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { screen = Screen.Edit(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add rule")
                }
            },
        ) { padding ->
            if (rules.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No rules yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create an if → then rule. Everything runs on this device — no account, no cloud.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { screen = Screen.Edit(null) }) { Text("Create rule") }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(padding),
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onToggle = {
                                store.setEnabled(rule.id, it)
                                reload()
                            },
                            onClick = { screen = Screen.Edit(rule) },
                            onDelete = {
                                store.delete(rule.id)
                                reload()
                            },
                        )
                    }
                }
            }
        }

        is Screen.Edit -> EditRuleScreen(
            initial = s.rule,
            onBack = { screen = Screen.List },
            onSave = { rule ->
                store.upsert(rule)
                reload()
                screen = Screen.List
            },
        )

        Screen.About -> AboutScreen(
            onBack = { screen = Screen.List },
            onExport = { exportText = store.exportJson() },
        )
    }

    exportText?.let { text ->
        AlertDialog(
            onDismissRequest = { exportText = null },
            title = { Text("Export rules (JSON)") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { exportText = null }) { Text("Close") }
            },
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    rule.triggerSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (rule.actions.isNotEmpty()) {
                    Text(
                        rule.actions.joinToString(" · ") { it.summary() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
    var actionType by remember {
        mutableStateOf(initial?.actions?.firstOrNull()?.type ?: ActionType.NOTIFY)
    }
    var actionText by remember {
        mutableStateOf(initial?.actions?.firstOrNull()?.text ?: "")
    }
    var actionTitle by remember {
        mutableStateOf(initial?.actions?.firstOrNull()?.title ?: "")
    }
    var ringer by remember {
        mutableStateOf(initial?.actions?.firstOrNull()?.ringerMode ?: RingerModeKind.VIBRATE)
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
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            EnumDropdown("When", TriggerType.entries, trigger) { trigger = it }

            when (trigger) {
                TriggerType.BATTERY_BELOW, TriggerType.BATTERY_ABOVE -> {
                    OutlinedTextField(
                        value = value.toString(),
                        onValueChange = { value = it.toIntOrNull()?.coerceIn(1, 100) ?: value },
                        label = { Text("Battery percent") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                TriggerType.TIME_DAILY -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = value.toString(),
                            onValueChange = { value = it.toIntOrNull()?.coerceIn(0, 23) ?: value },
                            label = { Text("Hour (0–23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = value2.toString(),
                            onValueChange = { value2 = it.toIntOrNull()?.coerceIn(0, 59) ?: value2 },
                            label = { Text("Minute") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
                else -> Unit
            }

            EnumDropdown("Then", ActionType.entries, actionType) { actionType = it }

            when (actionType) {
                ActionType.NOTIFY -> {
                    OutlinedTextField(
                        value = actionTitle,
                        onValueChange = { actionTitle = it },
                        label = { Text("Notification title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = actionText,
                        onValueChange = { actionText = it },
                        label = { Text("Notification text") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ActionType.TOAST -> {
                    OutlinedTextField(
                        value = actionText,
                        onValueChange = { actionText = it },
                        label = { Text("Toast message") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ActionType.RINGER_MODE -> {
                    EnumDropdown("Ringer mode", RingerModeKind.entries, ringer) { ringer = it }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val n = name.ifBlank {
                        when (trigger) {
                            TriggerType.BATTERY_BELOW -> "Low battery"
                            TriggerType.BATTERY_ABOVE -> "Battery charged"
                            TriggerType.CHARGER_CONNECTED -> "Charging"
                            TriggerType.CHARGER_DISCONNECTED -> "Unplugged"
                            TriggerType.HEADPHONES_CONNECTED -> "Headphones on"
                            TriggerType.HEADPHONES_DISCONNECTED -> "Headphones off"
                            TriggerType.BOOT_COMPLETED -> "On boot"
                            TriggerType.TIME_DAILY -> "Daily reminder"
                        }
                    }
                    val action = RuleAction(
                        type = actionType,
                        text = actionText,
                        title = actionTitle,
                        ringerMode = ringer,
                    )
                    onSave(
                        Rule(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            name = n,
                            enabled = initial?.enabled ?: true,
                            trigger = trigger,
                            triggerValue = value,
                            triggerValue2 = value2,
                            actions = listOf(action),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save rule") }
        }
    }
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
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.name.lowercase().replace('_', ' ')) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(onBack: () -> Unit, onExport: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Latch") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Latch ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
            Text(
                "Privacy-first automation for Android. Rules run only on your device. " +
                    "No account, no ads, no trackers, no cloud sync.",
            )
            Text("Triggers", style = MaterialTheme.typography.titleMedium)
            Text("Battery level · charger · headphones · daily time · boot")
            Text("Actions", style = MaterialTheme.typography.titleMedium)
            Text("Notification · ringer mode · toast")
            Text("Recipes are stored as a local JSON file and can be exported.")
            Button(onClick = onExport) { Text("Export rules JSON") }
            Text(
                "MIT · Project Endroid · Offline by design",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
