package com.sayanthrock.githubrock.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sayanthrock.githubrock.core.util.ChecksumVerifier
import com.sayanthrock.githubrock.data.local.DownloadDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val client: OkHttpClient,
    private val dao: DownloadDao
) : CoroutineWorker(context, params) {
    /**
     * Downloads the requested file, supports resuming partial downloads, verifies its checksum when provided,
     * and records progress and completion status.
     *
     * @return The successful, retry, or failure result based on the download outcome.
     */
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val name = inputData.getString(KEY_NAME)?.safeFileName() ?: return Result.failure()
        val expectedSha = inputData.getString(KEY_SHA256)
        val directory = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
        val resumedPath = inputData.getString(KEY_PARTIAL_PATH)?.let(::File)
            ?.takeIf { it.parentFile == directory && it.name.endsWith(".part") }
        val partial = resumedPath ?: File(directory, "$id-$name.part")
        val final = File(directory, "$id-$name")
        var knownTotal = 0L

        return try {
            val existing = partial.takeIf(File::exists)?.length() ?: 0L
            val request = Request.Builder().url(url).apply {
                if (existing > 0) header("Range", "bytes=$existing-")
            }.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    error("Download failed: HTTP ${response.code}")
                }
                val body = response.body ?: error("Empty download response")
                val append = existing > 0 && response.code == 206
                if (!append && partial.exists()) partial.delete()
                val startingBytes = if (append) existing else 0L
                knownTotal = body.contentLength()
                    .takeIf { it >= 0 }
                    ?.plus(startingBytes)
                    ?: 0L
                dao.updateProgress(
                    id = id,
                    status = "downloading",
                    downloaded = startingBytes,
                    total = knownTotal,
                    path = partial.absolutePath,
                    sha = null
                )
                copyResponseWithProgress(
                    id = id,
                    body = body.byteStream(),
                    target = partial,
                    append = append,
                    startingBytes = startingBytes,
                    totalBytes = knownTotal
                )
            }

            currentCoroutineContext().ensureActive()
            val sha = ChecksumVerifier.sha256(partial)
            if (expectedSha != null && !ChecksumVerifier.matches(sha, expectedSha)) {
                partial.delete()
                error("SHA-256 verification failed")
            }
            if (final.exists()) final.delete()
            check(partial.renameTo(final)) { "Unable to finalize download" }
            dao.updateProgress(id, "completed", final.length(), final.length(), final.absolutePath, sha)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val downloaded = partial.takeIf(File::exists)?.length() ?: 0L
            val willRetry = runAttemptCount < MAX_AUTOMATIC_RETRIES
            dao.updateProgress(
                id,
                if (willRetry) "retrying" else "failed",
                downloaded,
                knownTotal,
                partial.takeIf(File::exists)?.absolutePath,
                null
            )
            if (willRetry) Result.retry() else Result.failure()
        }
    }

    /**
     * Copies response data to the target file and periodically records download progress.
     *
     * @param id The download identifier.
     * @param body The response stream to copy.
     * @param target The file receiving the response data.
     * @param append Whether to append data to the target file.
     * @param startingBytes The number of bytes already downloaded.
     * @param totalBytes The expected total download size.
     */
    private suspend fun copyResponseWithProgress(
        id: Long,
        body: java.io.InputStream,
        target: File,
        append: Boolean,
        startingBytes: Long,
        totalBytes: Long
    ) {
        var downloaded = startingBytes
        var lastPublished = startingBytes
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        FileOutputStream(target, append).buffered().use { output ->
            body.use { input ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    downloaded += count
                    if (downloaded - lastPublished >= PROGRESS_UPDATE_BYTES) {
                        dao.updateProgress(id, "downloading", downloaded, totalBytes, target.absolutePath, null)
                        lastPublished = downloaded
                    }
                }
            }
        }
        dao.updateProgress(id, "downloading", downloaded, totalBytes, target.absolutePath, null)
    }

    /**
 * Replaces characters that are unsafe for filenames with underscores.
 *
 * @return A filename containing only letters, digits, periods, underscores, and hyphens.
 */
private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    companion object {
        const val KEY_ID = "download_id"
        const val KEY_URL = "download_url"
        const val KEY_NAME = "download_name"
        const val KEY_SHA256 = "download_sha256"
        const val KEY_PARTIAL_PATH = "download_partial_path"
        private const val PROGRESS_UPDATE_BYTES = 256 * 1024L
        private const val MAX_AUTOMATIC_RETRIES = 2

        /**
 * Creates the unique WorkManager name for a download.
 *
 * @param id The download identifier.
 * @return The standardized work name for the download.
 */
fun workName(id: Long): String = "github-rock-download-$id"
    }
}
