package dev.glycoguide.tv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.glycoguide.tv.player.PlayerManager
import dev.glycoguide.tv.util.AppSettings
import dev.glycoguide.tv.util.SettingsData

@Composable
fun SettingsScreen(
    settings: AppSettings,
) {
    val data = settings.data
    val availablePlayers = remember { PlayerManager.detectAvailablePlayers() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            "Einstellungen",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(24.dp))

        // === Player Settings ===
        SettingsSection("Video Player") {
            // Player command
            SettingsTextField(
                label = "Player-Befehl",
                value = data.playerCommand,
                onValueChange = { settings.update { copy(playerCommand = it) } },
                supportingText = "Verfügbar: ${availablePlayers.ifEmpty { listOf("keiner gefunden") }.joinToString(", ")}",
            )

            if (availablePlayers.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Kein Video-Player gefunden! Installiere mpv:\nsudo apt install mpv",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        // === Torrent Settings ===
        SettingsSection("Torrent") {
            SettingsTextField(
                label = "Download-Verzeichnis",
                value = data.downloadDirectory,
                onValueChange = { settings.update { copy(downloadDirectory = it) } },
            )

            SettingsNumberField(
                label = "Buffer-Größe (MB)",
                value = data.bufferSizeMB,
                onValueChange = { settings.update { copy(bufferSizeMB = it) } },
                supportingText = "Wie viel gepuffert wird bevor der Player startet",
            )

            SettingsNumberField(
                label = "Max Download-Speed (KB/s, 0 = unbegrenzt)",
                value = data.maxDownloadSpeedKBps,
                onValueChange = { settings.update { copy(maxDownloadSpeedKBps = it) } },
            )

            SettingsNumberField(
                label = "Max Upload-Speed (KB/s, 0 = unbegrenzt)",
                value = data.maxUploadSpeedKBps,
                onValueChange = { settings.update { copy(maxUploadSpeedKBps = it) } },
            )

            SettingsNumberField(
                label = "Max Verbindungen",
                value = data.maxConnections,
                onValueChange = { settings.update { copy(maxConnections = it) } },
            )

            SettingsSwitch(
                label = "Nach Download weiter seeden",
                checked = data.seedAfterDownload,
                onCheckedChange = { settings.update { copy(seedAfterDownload = it) } },
            )
        }

        // === Privacy Settings ===
        SettingsSection("Datenschutz") {
            SettingsSwitch(
                label = "VPN-Warnung vor Torrent-Streams anzeigen",
                subtitle = "Empfohlen! Warnt dich bevor du einen Torrent ohne VPN streamst.",
                checked = data.showVpnWarning,
                onCheckedChange = { settings.update { copy(showVpnWarning = it) } },
            )
        }

        // === Extensions Settings ===
        SettingsSection("Extensions") {
            SettingsTextField(
                label = "Extensions-Verzeichnis",
                value = data.extensionsDirectory,
                onValueChange = { settings.update { copy(extensionsDirectory = it) } },
                supportingText = "Verzeichnis für Provider JAR-Dateien",
            )
        }

        Spacer(Modifier.height(24.dp))

        // Reset button
        OutlinedButton(
            onClick = {
                settings.update { SettingsData() }
            },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Auf Standard zurücksetzen")
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

@Composable
private fun SettingsNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text ->
            text.toIntOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

@Composable
private fun SettingsSwitch(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}
