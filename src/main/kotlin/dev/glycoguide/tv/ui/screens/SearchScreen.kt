package dev.glycoguide.tv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import dev.glycoguide.tv.model.MediaItem
import dev.glycoguide.tv.model.SearchResult
import dev.glycoguide.tv.provider.ContentProvider
import dev.glycoguide.tv.provider.ProviderManager
import dev.glycoguide.tv.ui.components.MediaCard
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    providerManager: ProviderManager,
    onItemClick: (MediaItem) -> Unit,
) {
    val providers by providerManager.providers.collectAsState()
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Pair<ContentProvider, SearchResult>>>(emptyList()) }
    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun doSearch() {
        if (query.isBlank()) return
        scope.launch {
            isSearching = true
            currentPage = 1
            searchResults = providerManager.searchAll(query, 1)
            allItems = searchResults.flatMap { (provider, result) ->
                result.items.map { it.copy(providerId = provider.id) }
            }
            hasMore = searchResults.any { (_, result) -> result.hasMore }
            isSearching = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        scope.launch {
            isLoadingMore = true
            currentPage++
            val moreResults = providerManager.searchAll(query, currentPage)
            val newItems = moreResults.flatMap { (provider, result) ->
                result.items.map { it.copy(providerId = provider.id) }
            }
            allItems = allItems + newItems
            hasMore = moreResults.any { (_, result) -> result.hasMore }
            isLoadingMore = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            "Suche",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .onKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                        doSearch()
                        true
                    } else false
                },
            placeholder = { Text("Film oder Serie suchen...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Suchen")
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        searchResults = emptyList()
                        allItems = emptyList()
                        hasMore = false
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Löschen")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Search button
        Button(
            onClick = { doSearch() },
            enabled = query.isNotBlank() && !isSearching,
            modifier = Modifier.align(Alignment.End),
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Suchen")
        }

        Spacer(Modifier.height(16.dp))

        // Results
        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (allItems.isNotEmpty()) {
            if (allItems.isEmpty()) {
                EmptyState(
                    title = "Keine Ergebnisse",
                    message = "Keine Ergebnisse für \"$query\" gefunden.",
                )
            } else {
                Text(
                    "${allItems.size} Ergebnis${if (allItems.size != 1) "se" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(allItems, key = { "${it.providerId}_${it.id}" }) { item ->
                        MediaCard(
                            item = item,
                            onClick = { onItemClick(item) },
                        )
                    }

                    // Load more button
                    if (hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    OutlinedButton(onClick = { loadMore() }) {
                                        Text("Mehr laden")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (query.isNotBlank()) {
            EmptyState(
                title = "Suche starten",
                message = "Drücke Enter oder den Suchen-Button.",
            )
        }
    }
}
