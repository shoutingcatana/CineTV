package dev.glycoguide.tv.ui.screens

import androidx.compose.foundation.clickable
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
import dev.glycoguide.tv.provider.ContentProvider
import dev.glycoguide.tv.provider.ProviderManager
import dev.glycoguide.tv.util.AppSettings
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.Path

@Composable
fun ExtensionsScreen(
    providerManager: ProviderManager,
    settings: AppSettings,
) {
    val providers by providerManager.providers.collectAsState()
    val loadErrors by providerManager.loadErrors.collectAsState()
    var showInstallInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            "Extensions",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Verwalte Content-Provider für Film- und Serienquellen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val chooser = JFileChooser()
                    chooser.fileFilter = FileNameExtensionFilter("JAR Extensions", "jar")
                    chooser.dialogTitle = "Extension JAR auswählen"
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        providerManager.loadExtension(chooser.selectedFile.toPath())
                    }
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Extension laden")
            }

            OutlinedButton(
                onClick = {
                    providerManager.loadExtensionsFromDirectory(Path(settings.data.extensionsDirectory))
                },
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Neu laden")
            }

            OutlinedButton(onClick = { showInstallInfo = !showInstallInfo }) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Anleitung")
            }
        }

        // Install instructions
        if (showInstallInfo) {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Extension erstellen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Implementiere das ContentProvider-Interface\n" +
                        "2. Erstelle META-INF/services/dev.glycoguide.tv.provider.ContentProvider\n" +
                        "3. Trage den vollqualifizierten Klassennamen dort ein\n" +
                        "4. Baue als JAR und lege es im Extensions-Ordner ab:\n" +
                        "   ${settings.data.extensionsDirectory}\n" +
                        "5. Klicke auf 'Neu laden'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Load errors
        if (loadErrors.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            loadErrors.forEach { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))

        Text(
            "Installierte Provider (${providers.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))

        if (providers.isEmpty()) {
            EmptyState(
                title = "Keine Provider",
                message = "Lade eine Extension-JAR oder lege sie im Extensions-Ordner ab.",
            )
        } else {
            providers.forEach { provider ->
                ProviderCard(
                    provider = provider,
                    onRemove = if (provider.id != "demo") {
                        { providerManager.removeProvider(provider.id) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: ContentProvider,
    onRemove: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    provider.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        provider.language.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    provider.supportedTypes.forEach { type ->
                        Text(
                            type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (provider.id == "demo") {
                AssistChip(
                    onClick = {},
                    label = { Text("Built-in", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            onRemove?.let {
                IconButton(onClick = it) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Entfernen",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
