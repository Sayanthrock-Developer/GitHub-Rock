package com.sayanthrock.githubrock.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sayanthrock.githubrock.core.util.ChecksumVerifier
import com.sayanthrock.githubrock.data.local.DownloadDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val client: OkHttpClient,
    private val dao: DownloadDao
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val name = inputData.getString(KEY_NAME)?.safeFileName() ?: return Result.failure()
        val expectedSha = inputData.getString(KEY_SHA256)
        val directory = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
        val partial = File(directory, "$name.part")
        val final = File(directory, name)

        return runCatching {
            val existing = partial.takeIf(File::exists)?.length() ?: 0L
            val request = Request.Builder().url(url).apply {
                if (existing > 0) header("Range", "bytes=$existing-")
            }.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) error("Download failed: HTTP ${response.code}")
                val body = response.body ?: error("Empty download response")
                val append = existing > 0 && response.code == 206
                if (!append && partial.exists()) partial.delete()
                java.io.FileOutputStream(partial, append).buffered().use { output ->
                    body.byteStream().use { it.copyTo(output) }
                }
                val sha = ChecksumVerifier.sha256(partial)
                if (expectedSha != null && !ChecksumVerifier.matches(sha, expectedSha)) {
                    partial.delete()
                    error("SHA-256 verification failed")
                }
                if (final.exists()) final.delete()
                check(partial.renameTo(final)) { "Unable to finalize download" }
                dao.updateProgress(id, "completed", final.length(), final.length(), final.absolutePath, sha)
            }
            Result.success()
        }.getOrElse {
            dao.updateProgress(id, "failed", partial.length(), 0, partial.absolutePath, null)
            if (runAttemptCount >= 2) Result.failure() else Result.retry()
        }
    }

    private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    companion object {
        const val KEY_ID = "download_id"
        const val KEY_URL = "download_url"
        const val KEY_NAME = "download_name"
        const val KEY_SHA256 = "download_sha256"
    }
}
