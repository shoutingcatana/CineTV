package dev.glycoguide.tv.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

private val imageCache = ConcurrentHashMap<String, ImageBitmap>()

@Composable
fun AsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url == null) {
        ImagePlaceholder(modifier)
        return
    }

    var imageBitmap by remember(url) { mutableStateOf(imageCache[url]) }
    var isLoading by remember(url) { mutableStateOf(imageBitmap == null) }
    var isError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (imageBitmap != null) return@LaunchedEffect
        isLoading = true
        isError = false
        withContext(Dispatchers.IO) {
            try {
                val connection = URI(url).toURL().openConnection()
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("User-Agent", "CineStream/1.0")
                val bytes = connection.inputStream.use { it.readBytes() }
                val bitmap = loadImageBitmap(ByteArrayInputStream(bytes))
                imageCache[url] = bitmap
                imageBitmap = bitmap
            } catch (_: Exception) {
                isError = true
            }
        }
        isLoading = false
    }

    when {
        imageBitmap != null -> Image(
            bitmap = imageBitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        isLoading -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp).align(Alignment.Center),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        else -> ImagePlaceholder(modifier)
    }
}

@Composable
private fun ImagePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        Icon(
            Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(32.dp).align(Alignment.Center),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}
