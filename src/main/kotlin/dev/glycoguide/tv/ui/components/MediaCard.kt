package dev.glycoguide.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.glycoguide.tv.model.MediaItem
import dev.glycoguide.tv.model.MediaType
import dev.glycoguide.tv.util.AsyncImage

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        // Poster image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                url = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
            )

            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    ),
            )

            // Rating badge
            item.rating?.let { rating ->
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                rating >= 7.5 -> Color(0xFF22C55E)
                                rating >= 5.0 -> Color(0xFFEAB308)
                                else -> Color(0xFFEF4444)
                            }.copy(alpha = 0.9f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "%.1f".format(rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            // Type badge
            if (item.type != MediaType.MOVIE) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        when (item.type) {
                            MediaType.TV_SHOW -> "Serie"
                            MediaType.ANIME -> "Anime"
                            MediaType.DOCUMENTARY -> "Doku"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            // Year at bottom of poster
            item.year?.let { year ->
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                )
            }
        }

        // Title below poster
        Text(
            item.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}
