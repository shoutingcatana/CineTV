package dev.glycoguide.tv.torrent

import dev.glycoguide.tv.model.TorrentSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

enum class TorrentState {
    INITIALIZING, METADATA, BUFFERING, READY, DOWNLOADING, SEEDING, PAUSED, ERROR, STOPPED
}

data class TorrentStatus(
    val state: TorrentState = TorrentState.INITIALIZING,
    val progress: Float = 0f,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val seeds: Int = 0,
    val peers: Int = 0,
    val bufferProgress: Float = 0f,
    val videoFilePath: String? = null,
    val errorMessage: String? = null,
)

class TorrentStream(
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow(TorrentStatus())
    val status: StateFlow<TorrentStatus> = _status.asStateFlow()

    internal fun updateStatus(update: TorrentStatus.() -> TorrentStatus) {
        _status.value = _status.value.update()
    }

    fun stop() {
        _status.value = _status.value.copy(state = TorrentState.STOPPED)
        scope.cancel()
    }
}

class TorrentEngine(private val getSettings: () -> TorrentSettings) {
    private var sessionManager: Any? = null
    private var isAvailable = false
    private val activeStreams = mutableListOf<TorrentStream>()

    fun start() {
        try {
            // Try to load libtorrent4j via reflection to handle missing native library gracefully
            val smClass = Class.forName("org.libtorrent4j.SessionManager")
            val sm = smClass.getDeclaredConstructor(Boolean::class.java).newInstance(false)

            // Configure settings pack before starting
            try {
                val spClass = Class.forName("org.libtorrent4j.swig.settings_pack")
                val sp = spClass.getDeclaredConstructor().newInstance()
                val intSettingsClass = Class.forName("org.libtorrent4j.swig.settings_pack\$int_types")

                fun setIntSetting(settingName: String, value: Int) {
                    try {
                        val field = intSettingsClass.getField(settingName)
                        val settingEnum = field.get(null)
                        spClass.getMethod("set_int", intSettingsClass, Int::class.java)
                            .invoke(sp, settingEnum, value)
                    } catch (_: Exception) {}
                }

                val data = getSettings()
                if (data.maxDownloadSpeedKBps > 0) {
                    setIntSetting("download_rate_limit", data.maxDownloadSpeedKBps * 1024)
                }
                if (data.maxUploadSpeedKBps > 0) {
                    setIntSetting("upload_rate_limit", data.maxUploadSpeedKBps * 1024)
                }
                if (data.maxConnections > 0) {
                    setIntSetting("connections_limit", data.maxConnections)
                }

                val startWithSpMethod = smClass.getMethod("start", spClass)
                startWithSpMethod.invoke(sm, sp)
            } catch (_: Exception) {
                // If settings_pack configuration fails, start without custom settings
                val startMethod = smClass.getMethod("start")
                startMethod.invoke(sm)
            }

            sessionManager = sm
            isAvailable = true
            println("TorrentEngine: libtorrent4j started successfully")
        } catch (e: Throwable) {
            System.err.println("TorrentEngine: libtorrent4j not available - ${e.message}")
            System.err.println("TorrentEngine: Torrent streaming will not be available.")
            System.err.println("TorrentEngine: Direct streams will still work.")
            isAvailable = false
        }
    }

    fun isAvailable(): Boolean = isAvailable

    fun stop() {
        activeStreams.forEach { it.stop() }
        activeStreams.clear()
        try {
            sessionManager?.let { sm ->
                sm::class.java.getMethod("stop").invoke(sm)
            }
        } catch (_: Exception) { }
    }

    suspend fun streamMagnet(magnetUrl: String): TorrentStream {
        val streamScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val stream = TorrentStream(streamScope)

        if (!isAvailable || sessionManager == null) {
            stream.updateStatus {
                copy(
                    state = TorrentState.ERROR,
                    errorMessage = "Torrent engine not available. Install libtorrent4j native library."
                )
            }
            return stream
        }

        val sm = sessionManager!!
        val saveDir = File(getSettings().downloadDirectory)
        saveDir.mkdirs()

        stream.updateStatus { copy(state = TorrentState.METADATA) }

        streamScope.launch {
            try {
                val smClass = sm::class.java

                // Fetch magnet metadata (timeout 60s)
                val fetchMethod = smClass.getMethod("fetchMagnet", String::class.java, Int::class.java, File::class.java)
                val data = fetchMethod.invoke(sm, magnetUrl, 60, saveDir) as? ByteArray

                if (data == null) {
                    stream.updateStatus {
                        copy(state = TorrentState.ERROR, errorMessage = "Failed to fetch torrent metadata (timeout)")
                    }
                    return@launch
                }

                // Create TorrentInfo from the metadata bytes
                val tiClass = Class.forName("org.libtorrent4j.TorrentInfo")
                val bdecodeMethod = tiClass.getMethod("bdecode", ByteArray::class.java)
                val torrentInfo = bdecodeMethod.invoke(null, data)

                // Find the video file in the torrent
                val videoPath = findVideoFile(torrentInfo, saveDir.toPath())
                if (videoPath == null) {
                    stream.updateStatus {
                        copy(state = TorrentState.ERROR, errorMessage = "No video file found in torrent")
                    }
                    return@launch
                }

                stream.updateStatus { copy(videoFilePath = videoPath.toString()) }

                // Start downloading
                val downloadMethod = smClass.getMethod("download", tiClass, File::class.java)
                downloadMethod.invoke(sm, torrentInfo, saveDir)

                delay(1500) // Wait for handle registration

                // Get torrent handle
                val infoHashMethod = tiClass.getMethod("infoHash")
                val infoHash = infoHashMethod.invoke(torrentInfo)
                val findMethod = smClass.getMethod("find", infoHash::class.java)
                val handle = findMethod.invoke(sm, infoHash)

                if (handle == null) {
                    stream.updateStatus {
                        copy(state = TorrentState.ERROR, errorMessage = "Failed to start download")
                    }
                    return@launch
                }

                // Enable sequential download
                enableSequentialDownload(handle)

                stream.updateStatus { copy(state = TorrentState.BUFFERING) }

                // Monitor progress
                val bufferThresholdBytes = getSettings().bufferSizeMB * 1024L * 1024L
                val handleClass = handle::class.java
                val statusMethod = handleClass.getMethod("status")

                while (isActive) {
                    delay(1000)
                    val status = statusMethod.invoke(handle)
                    val statusClass = status::class.java

                    val progress = statusClass.getMethod("progress").invoke(status) as Float
                    val totalDone = statusClass.getMethod("totalDone").invoke(status) as Long
                    val dlRate = statusClass.getMethod("downloadPayloadRate").invoke(status) as Int
                    val ulRate = statusClass.getMethod("uploadPayloadRate").invoke(status) as Int
                    val numSeeds = statusClass.getMethod("numSeeds").invoke(status) as Int
                    val numPeers = statusClass.getMethod("numPeers").invoke(status) as Int

                    val bufferReady = totalDone >= bufferThresholdBytes || progress >= 1.0f
                    val currentState = stream.status.value.state

                    stream.updateStatus {
                        copy(
                            state = when {
                                progress >= 1.0f -> TorrentState.SEEDING
                                bufferReady && currentState == TorrentState.BUFFERING -> TorrentState.READY
                                bufferReady -> TorrentState.READY
                                else -> currentState
                            },
                            progress = progress,
                            downloadSpeed = dlRate.toLong(),
                            uploadSpeed = ulRate.toLong(),
                            seeds = numSeeds,
                            peers = numPeers,
                            bufferProgress = if (bufferThresholdBytes > 0) {
                                (totalDone.toFloat() / bufferThresholdBytes).coerceAtMost(1f)
                            } else 1f,
                        )
                    }

                    // Stop seeding if user disabled it
                    if (progress >= 1.0f && !getSettings().seedAfterDownload) {
                        try {
                            handleClass.getMethod("pause").invoke(handle)
                        } catch (_: Exception) {}
                        break
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                stream.updateStatus {
                    copy(state = TorrentState.ERROR, errorMessage = e.message ?: "Unknown torrent error")
                }
            }
        }

        activeStreams.add(stream)
        return stream
    }

    private fun findVideoFile(torrentInfo: Any, saveDir: Path): Path? {
        val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "ts")
        try {
            val tiClass = torrentInfo::class.java
            val filesMethod = tiClass.getMethod("files")
            val fileStorage = filesMethod.invoke(torrentInfo)
            val fsClass = fileStorage::class.java
            val numFilesMethod = fsClass.getMethod("numFiles")
            val numFiles = numFilesMethod.invoke(fileStorage) as Int
            val fileNameMethod = fsClass.getMethod("fileName", Int::class.java)
            val fileSizeMethod = fsClass.getMethod("fileSize", Int::class.java)
            val filePathMethod = fsClass.getMethod("filePath", Int::class.java)

            var largestSize = 0L
            var largestPath: Path? = null

            for (i in 0 until numFiles) {
                val fileName = fileNameMethod.invoke(fileStorage, i) as String
                val fileSize = fileSizeMethod.invoke(fileStorage, i) as Long
                val ext = fileName.substringAfterLast('.', "").lowercase()

                if (ext in videoExtensions && fileSize > largestSize) {
                    largestSize = fileSize
                    val filePath = filePathMethod.invoke(fileStorage, i) as String
                    largestPath = saveDir.resolve(filePath)
                }
            }

            return largestPath
        } catch (e: Exception) {
            System.err.println("TorrentEngine: Error finding video file: ${e.message}")
            return null
        }
    }

    private fun enableSequentialDownload(handle: Any) {
        try {
            val handleClass = handle::class.java
            val flagsMethod = handleClass.getMethod("flags")
            val currentFlags = flagsMethod.invoke(handle)

            val tfClass = Class.forName("org.libtorrent4j.TorrentFlags")
            val seqField = tfClass.getField("SEQUENTIAL_DOWNLOAD")
            val seqFlag = seqField.get(null)

            val orMethod = currentFlags::class.java.getMethod("or_", seqFlag::class.java)
            val newFlags = orMethod.invoke(currentFlags, seqFlag)

            val setFlagsMethod = handleClass.getMethod("setFlags", newFlags::class.java)
            setFlagsMethod.invoke(handle, newFlags)
        } catch (e: Exception) {
            System.err.println("TorrentEngine: Could not enable sequential download: ${e.message}")
        }
    }
}
