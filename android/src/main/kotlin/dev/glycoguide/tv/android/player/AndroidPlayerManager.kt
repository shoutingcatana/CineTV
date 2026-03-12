package dev.glycoguide.tv.android.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider

/**
 * Android video player — launches an external video player via Intent.
 * For embed URLs (VidSrc/2Embed etc), opens in browser where the
 * built-in player or user's player app handles the video.
 * For direct media files, launches a video player intent (VLC, MX Player, etc.)
 */
class AndroidPlayerManager(private val context: Context) {

    private val directExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "m4v", "ts", "m3u8", "mpd")

    /** Check if URL points to a direct media file vs. an embed page */
    private fun isDirectMedia(url: String): Boolean {
        val lower = url.lowercase()
        return directExtensions.any { lower.endsWith(".$it") }
                || lower.contains("/stream")
                || lower.startsWith("magnet:")
    }

    fun playUrl(url: String, title: String = "") {
        if (isDirectMedia(url)) {
            // Direct media → launch video player
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "video/*")
                putExtra(Intent.EXTRA_TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // No video player → open in browser as fallback
                openInBrowser(url)
            }
        } else {
            // Embed URL (VidSrc, 2Embed, etc.) → open in browser
            // The embed page contains its own video player that works in-browser
            openInBrowser(url)
        }
    }

    fun playFile(path: String, title: String = "") {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("file://$path"), "video/*")
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            openInBrowser("file://$path")
        }
    }

    private fun openInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        fun hasExternalPlayer(context: Context): Boolean {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("content://test"), "video/*")
            }
            return intent.resolveActivity(context.packageManager) != null
        }
    }
}
