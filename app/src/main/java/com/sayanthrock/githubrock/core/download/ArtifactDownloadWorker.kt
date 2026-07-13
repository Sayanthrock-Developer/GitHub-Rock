package com.sayanthrock.githubrock.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sayanthrock.githubrock.core.util.Checksum
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Named

@HiltWorker
class ArtifactDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    @Named("downloadClient") private val client: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Downloads an HTTPS artifact, optionally verifies its SHA-256 checksum, and reports the result.
     *
     * @return A successful result containing the downloaded file metadata, a retry result for recoverable I/O failures, or a failure result with an error message.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL).orEmpty()
        val expectedSha = inputData.getString(KEY_EXPECTED_SHA)?.trim()?.lowercase().orEmpty()
        if (!url.startsWith("https://")) return@withContext failure("Only HTTPS downloads are allowed.")

        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext failure("Download failed (${response.code}).")
                val body = response.body ?: return@withContext failure("The server returned an empty file.")
                val directory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: applicationContext.filesDir
                directory.mkdirs()

                val suggestedName = url.substringAfterLast('/').substringBefore('?')
                    .takeIf { it.isNotBlank() } ?: "artifact-${System.currentTimeMillis()}"
                val safeName = suggestedName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val destination = uniqueFile(directory, safeName)
                val total = body.contentLength()
                var copied = 0L

                body.byteStream().use { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            if (isStopped) {
                                destination.delete()
                                return@withContext Result.failure()
                            }
                            val count = input.read(buffer)
                            if (count <= 0) break
                            output.write(buffer, 0, count)
                            copied += count
                            if (total > 0) {
                                val progress = ((copied * 100L) / total).toInt().coerceIn(0, 100)
                                setProgress(workDataOf(KEY_PROGRESS to progress, KEY_BYTES to copied))
                                showProgressNotification(progress, safeName)
                            }
                        }
                    }
                }

                val actualSha = Checksum.sha256(destination)
                if (expectedSha.isNotBlank() && !actualSha.equals(expectedSha, ignoreCase = true)) {
                    destination.delete()
                    return@withContext failure("SHA-256 verification failed.")
                }

                showCompletedNotification(safeName)
                Result.success(
                    workDataOf(
                        KEY_FILE_PATH to destination.absolutePath,
                        KEY_ACTUAL_SHA to actualSha,
                        KEY_FILE_NAME to destination.name,
                        KEY_FILE_SIZE to destination.length()
                    )
                )
            }
        } catch (error: IOException) {
            if (runAttemptCount < 3) Result.retry() else failure(error.message ?: "Download failed.")
        }
    }

    /**
     * Creates a file path that avoids overwriting an existing file.
     *
     * @param directory The directory in which to create the file path.
     * @param name The proposed file name.
     * @return The original file path if it is unused; otherwise, a timestamped file path.
     */
    private fun uniqueFile(directory: File, name: String): File {
        val original = File(directory, name)
        if (!original.exists()) return original
        val base = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "")
        val suffix = if (extension.isBlank()) "" else ".$extension"
        return File(directory, "$base-${System.currentTimeMillis()}$suffix")
    }

    /**
 * Creates a failed work result containing an error message.
 *
 * @param message The error message to include in the result.
 * @return A failed work result with the specified error message.
 */
private fun failure(message: String): Result = Result.failure(workDataOf(KEY_ERROR to message))

    /**
     * Updates the download notification with the current progress.
     *
     * @param progress The download progress percentage.
     * @param fileName The name of the file being downloaded.
     */
    private fun showProgressNotification(progress: Int, fileName: String) {
        val manager = notificationManager()
        manager.notify(
            id.hashCode(),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading $fileName")
                .setProgress(100, progress, false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build()
        )
    }

    /**
     * Shows a notification indicating that the download is complete.
     *
     * @param fileName The name of the downloaded file.
     */
    private fun showCompletedNotification(fileName: String) {
        notificationManager().notify(
            id.hashCode(),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download complete")
                .setContentText(fileName)
                .setAutoCancel(true)
                .build()
        )
    }

    /**
     * Retrieves the notification manager and ensures the artifact download notification channel exists.
     *
     * @return The application's notification manager.
     */
    private fun notificationManager(): NotificationManager {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Artifact downloads", NotificationManager.IMPORTANCE_LOW)
        )
        return manager
    }

    companion object {
        const val TAG = "github-rock-download"
        const val KEY_URL = "url"
        const val KEY_EXPECTED_SHA = "expected_sha"
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES = "bytes"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_FILE_SIZE = "file_size"
        const val KEY_ACTUAL_SHA = "actual_sha"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "artifact_downloads"

        /**
         * Creates the input data required to configure an artifact download.
         *
         * @param url The HTTPS URL of the artifact to download.
         * @param expectedSha The expected SHA-256 checksum, or an empty string to skip verification.
         * @return WorkManager input data containing the download URL and expected checksum.
         */
        fun input(url: String, expectedSha: String): Data = workDataOf(
            KEY_URL to url,
            KEY_EXPECTED_SHA to expectedSha
        )
    }
}
