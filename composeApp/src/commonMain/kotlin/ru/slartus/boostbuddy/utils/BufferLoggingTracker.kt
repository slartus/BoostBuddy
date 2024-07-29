package ru.slartus.boostbuddy.utils


import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readString
import kotlinx.io.writeString

class BufferLoggingTracker(private val debugLog: Boolean) : Antilog() {
    private val buffer = Buffer()
    private val properties = mutableMapOf<String, Any>()
    private val mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tempFilePath = Path(SystemTemporaryDirectory, "buffer.log")

    override fun isEnable(priority: LogLevel, tag: String?): Boolean {
        return debugLog
    }

    init {
        coroutineScope.launch {
            readBufferFromFile()
        }
    }

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        coroutineScope.launch {
            addToBuffer(buildString {
                append("[${priority.name}]")
                if (tag != null)
                    append(" ").append("[$tag]")
                if (message != null)
                    append(" ").append(message)
                if (throwable != null)
                    append(" ").append(throwable.toString())
            })
        }
    }

    private suspend fun addToBuffer(data: String) {
        val now = now()
        mutex.withLock {
            val currentValue = buffer.readString()
            val newValue =
                "[${formatInstant(now)}] $data\n$currentValue".let {
                    if (it.length > MAX_LENGTH) it.substring(0, MAX_LENGTH)
                    else it
                }
            buffer.writeString(newValue)
            writeBufferToFile()
        }
    }

    private suspend fun writeBufferToFile() = withContext(Dispatchers.IO) {
        runCatching {
            val file = SystemFileSystem.sink(tempFilePath, append = false)
            runCatching {
                val log = buffer.readString()
                buffer.writeString(log)
                file.write(buffer, buffer.size)
                file.flush()
                buffer.writeString(log)
            }
            file.close()
        }
    }

    private suspend fun readBufferFromFile() = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                SystemFileSystem.metadataOrNull(tempFilePath)?.let { metadata ->
                    val rawSource = SystemFileSystem.source(tempFilePath)
                    buffer.write(rawSource, metadata.size)
                    val log = buffer.readString()
                    buffer.writeString(log)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    suspend fun getLog(): String = mutex.withLock {
        val log = buffer.readString()
        buffer.writeString(log)
        val propertiesString =
            properties.map { "${it.key}=${it.value}" }.joinToString(separator = "\n")
        "$propertiesString\n${log}"
    }

    fun getLogPath(): Path = tempFilePath

    private companion object {
        const val MAX_LENGTH = 1_000_000
        private inline fun now(): Instant = Clock.System.now()
        private inline fun formatInstant(instant: Instant): String =
            instant.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)

        private inline fun formatNow(): String = formatInstant(now())
    }
}