package dev.glycoguide.tv.player

import dev.glycoguide.tv.util.AppSettings
import java.io.File

class PlayerManager(private val settings: AppSettings) {

    fun play(filePath: String) {
        val command = settings.data.playerCommand.trim()
        val file = File(filePath)

        // Try the configured player first
        if (tryLaunch(command, filePath)) return

        // Fallback chain: try common video players
        val fallbacks = listOf("mpv", "vlc", "celluloid", "totem", "xdg-open")
        for (player in fallbacks) {
            if (player != command && tryLaunch(player, filePath)) return
        }

        throw IllegalStateException(
            "No video player found. Please install mpv or VLC:\n" +
            "  sudo apt install mpv    (recommended)\n" +
            "  sudo apt install vlc"
        )
    }

    fun playUrl(url: String) {
        val command = settings.data.playerCommand.trim()

        // For embed URLs, first try to resolve via yt-dlp, then fall back to direct player
        val resolvedUrl = resolveStreamUrl(url) ?: url

        if (tryLaunch(command, resolvedUrl)) return

        val fallbacks = listOf("mpv", "vlc", "xdg-open")
        for (player in fallbacks) {
            if (player != command && tryLaunch(player, resolvedUrl)) return
        }

        throw IllegalStateException(
            "No video player found. Please install mpv or VLC."
        )
    }

    /**
     * Try to resolve an embed page URL to an actual stream URL using yt-dlp.
     * Returns the resolved URL, or null if yt-dlp is not available or fails.
     */
    private fun resolveStreamUrl(url: String): String? {
        // If it's already a direct media URL, no need to resolve
        val lower = url.lowercase()
        if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".m3u8")
            || lower.endsWith(".webm") || lower.contains("/stream") || lower.startsWith("magnet:")) {
            return null
        }

        return try {
            val process = ProcessBuilder("yt-dlp", "--no-warnings", "-g", url)
                .redirectErrorStream(false)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0 && output.isNotBlank()) {
                // yt-dlp may return multiple URLs (video + audio), take the first
                output.lines().firstOrNull()?.trim()
            } else {
                null
            }
        } catch (_: Exception) {
            null // yt-dlp not installed, skip resolution
        }
    }

    private fun tryLaunch(command: String, target: String): Boolean {
        return try {
            val parts = command.split(" ") + target
            ProcessBuilder(parts)
                .redirectErrorStream(true)
                .start()
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        fun detectAvailablePlayers(): List<String> {
            val players = listOf("mpv", "vlc", "celluloid", "totem")
            return players.filter { player ->
                try {
                    val process = ProcessBuilder("which", player)
                        .redirectErrorStream(true)
                        .start()
                    process.waitFor() == 0
                } catch (_: Exception) {
                    false
                }
            }
        }

        fun isYtdlpAvailable(): Boolean {
            return try {
                val process = ProcessBuilder("which", "yt-dlp")
                    .redirectErrorStream(true)
                    .start()
                process.waitFor() == 0
            } catch (_: Exception) {
                false
            }
        }
    }
}
