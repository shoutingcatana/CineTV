package dev.glycoguide.tv.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.glycoguide.tv.model.Category
import dev.glycoguide.tv.model.MediaItem
import dev.glycoguide.tv.provider.ProviderManager
import dev.glycoguide.tv.ui.components.MediaCard
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    providerManager: ProviderManager,
    onItemClick: (MediaItem) -> Unit,
) {
    val providers by providerManager.providers.collectAsState()
    var homeContent by remember { mutableStateOf<List<Pair<String, List<Category>>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(providers) {
        isLoading = true
        errorMessage = null
        try {
            homeContent = providerManager.getHomeAll()
        } catch (e: Exception) {
            errorMessage = e.message
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        // Header
        Text(
            "Home",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))

        if (providers.isEmpty()) {
            EmptyState(
                title = "Keine Provider installiert",
                message = "Gehe zu Extensions um Content-Provider hinzuzufügen.",
            )
            return@Column
        }

        when {
            isLoading -> {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            errorMessage != null -> {
                ErrorBanner(errorMessage!!) {
                    scope.launch {
                        isLoading = true
                        homeContent = providerManager.getHomeAll()
                        isLoading = false
                    }
                }
            }
            else -> {
                homeContent.forEach { (providerName, categories) ->
                    if (categories.isNotEmpty()) {
                        Text(
                            providerName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )

                        categories.forEach { category ->
                            CategoryRow(
                                category = category,
                                onItemClick = onItemClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRow(
    category: Category,
    onItemClick: (MediaItem) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            category.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            category.items.forEach { item ->
                MediaCard(
                    item = item,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
