package com.sayanthrock.githubrock.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.sayanthrock.githubrock.R
import com.sayanthrock.githubrock.core.util.ChecksumVerifier
import com.sayanthrock.githubrock.core.util.inspectApk
import com.sayanthrock.githubrock.data.local.DownloadState
import com.sayanthrock.githubrock.data.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Named
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Single download engine for GitHub release assets and Actions artifacts. */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    @Named("downloadClient") private val client: OkHttpClient,
    private val repository: DownloadRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        val url = inputData.getString(KEY_URL)?.trim()?.takeIf(String::isNotBlank) ?: return Result.failure()
        val name = inputData.getString(KEY_NAME)?.safeFileName()?.takeIf(String::isNotBlank) ?: return Result.failure()
        val expectedSha = inputData.getString(KEY_SHA256)?.trim()?.takeIf(String::isNotBlank)
        val expectedPackage = inputData.getString(KEY_EXPECTED_PACKAGE)?.takeIf(String::isNotBlank)
        val directory = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
        val resumedPath = inputData.getString(KEY_PARTIAL_PATH)?.let(::File)
            ?.takeIf { it.canonicalFile.parentFile == directory.canonicalFile && it.name.endsWith(".part") }
        val partial = resumedPath ?: File(directory, "$id-$name.part")
        val final = File(directory, "$id-$name")
        var knownTotal = 0L

        setForeground(downloadForegroundInfo(id, name, 0L, 0L))

        return try {
            var existing = partial.takeIf(File::exists)?.length() ?: 0L
            var response: Response? = null
            var restarted = false

            while (true) {
                response?.close()
                response = executeDownload(url, existing)
                when {
                    response.code == 416 && existing > 0L && !restarted -> {
                        response.close(); partial.delete(); existing = 0L; restarted = true
                    }
                    response.code == 206 && existing > 0L -> {
                        val range = response.header("Content-Range")?.let(::parseContentRange)
                        if (range == null || range.first != existing || range.third <= existing) {
                            response.close(); partial.delete(); existing = 0L
                            if (restarted) error("Server returned an invalid byte range")
                            restarted = true
                        } else break
                    }
                    response.code == 200 -> {
                        if (existing > 0L) {
                            response.close(); partial.delete(); existing = 0L
                            if (restarted) error("Download server ignored resume request")
                            restarted = true; continue
                        }
                        break
                    }
                    response.isSuccessful -> {
                        if (existing > 0L) {
                            response.close(); partial.delete(); existing = 0L
                            if (restarted) error("Unexpected response while resuming download")
                            restarted = true; continue
                        }
                        break
                    }
                    else -> break
                }
            }

            response!!.use { result ->
                if (!result.isSuccessful) error("Download failed: HTTP ${result.code}")
                val body = result.body ?: error("Empty download response")
                val contentType = body.contentType()?.toString()?.lowercase().orEmpty()
                if (name.endsWith(".apk", ignoreCase = true) &&
                    (contentType.contains("text/html") || contentType.contains("text/plain") || contentType.contains("application/json"))) {
                    error("GitHub returned a non-binary response ($contentType)")
                }

                val append = existing > 0L && result.code == 206
                val range = if (append) result.header("Content-Range")?.let(::parseContentRange) else null
                if (append && (range == null || range.first != existing)) error("Invalid resume range")
                if (!append && partial.exists()) partial.delete()

                val startingBytes = if (append) existing else 0L
                knownTotal = when {
                    range != null -> range.third
                    body.contentLength() >= 0L -> startingBytes + body.contentLength()
                    else -> 0L
                }
                repository.updateProgress(id, DownloadState.DOWNLOADING, startingBytes, knownTotal, partial.absolutePath, expectedSha)
                setForeground(downloadForegroundInfo(id, name, startingBytes, knownTotal))
                copyResponseWithProgress(id, name, body.byteStream(), partial, append, startingBytes, knownTotal, expectedSha)
            }

            currentCoroutineContext().ensureActive()
            if (knownTotal > 0L && partial.length() != knownTotal) {
                error("Download size mismatch: ${partial.length()} of $knownTotal bytes")
            }

            repository.updateProgress(id, DownloadState.VERIFYING, partial.length(), knownTotal, partial.absolutePath, expectedSha)
            if (name.endsWith(".apk", ignoreCase = true)) {
                val previous = expectedPackage?.let { repository.latestCompletedForPackage(it) }
                val inspection = inspectApk(
                    context = applicationContext,
                    file = partial,
                    expectedPackage = expectedPackage,
                    previousVersionCode = previous?.versionCode,
                    previousPermissions = previous?.permissions?.split("\n")?.filter(String::isNotBlank).orEmpty(),
                    previousCertificateSha256 = previous?.certificateSha256
                )
                repository.updateSecurity(
                    id = id,
                    packageName = inspection.packageName,
                    versionCode = inspection.versionCode,
                    versionName = inspection.versionName,
                    minSdk = inspection.minSdk,
                    targetSdk = inspection.targetSdk,
                    permissions = inspection.permissions.joinToString("\n"),
                    certificateSha256 = inspection.certificateSha256,
                    signatureSchemes = inspection.signatureSchemes.joinToString(","),
                    architectures = inspection.architectures.joinToString(","),
                    securityRisk = if (inspection.riskReasons.isEmpty()) "low" else "review",
                    securityReasons = inspection.riskReasons.joinToString("\n")
                )
            }

            val sha = ChecksumVerifier.sha256(partial)
            if (expectedSha != null && !ChecksumVerifier.matches(sha, expectedSha)) {
                partial.delete()
                error("SHA-256 verification failed")
            }
            if (final.exists()) final.delete()
            check(partial.renameTo(final)) { "Unable to finalize download" }
            check(final.isFile && final.length() > 0L) { "Final download file is unavailable" }
            val terminalState = if (name.endsWith(".apk", ignoreCase = true)) DownloadState.INSTALLABLE else DownloadState.COMPLETED
            repository.updateProgress(id, terminalState, final.length(), final.length(), final.absolutePath, sha, 0L, 0L, null)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val downloaded = partial.takeIf(File::exists)?.length() ?: 0L
            val willRetry = runAttemptCount < MAX_AUTOMATIC_RETRIES
            repository.updateProgress(
                id,
                if (willRetry) DownloadState.RETRYING else DownloadState.FAILED,
                downloaded,
                knownTotal,
                partial.takeIf(File::exists)?.absolutePath,
                expectedSha,
                0L,
                null,
                error.message ?: "Download failed"
            )
            if (willRetry) Result.retry() else Result.failure()
        }
    }

    private fun executeDownload(url: String, existing: Long): Response {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GitHub-Rock/1.0")
            .header("Accept", "application/octet-stream")
            .header("Accept-Encoding", "identity")
            .apply { if (existing > 0L) header("Range", "bytes=$existing-") }
            .build()
        return client.newCall(request).execute()
    }

    private suspend fun copyResponseWithProgress(
        id: Long,
        fileName: String,
        body: java.io.InputStream,
        target: File,
        append: Boolean,
        startingBytes: Long,
        totalBytes: Long,
        expectedSha: String?
    ) {
        var downloaded = startingBytes
        var lastPublished = startingBytes
        var lastSampleBytes = startingBytes
        var lastSampleAt = System.nanoTime()
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
                        val now = System.nanoTime()
                        val elapsedSeconds = (now - lastSampleAt) / 1_000_000_000.0
                        val speed = if (elapsedSeconds > 0.0) ((downloaded - lastSampleBytes) / elapsedSeconds).toLong() else 0L
                        val eta = if (speed > 0L && totalBytes > downloaded) (totalBytes - downloaded + speed - 1L) / speed else null
                        repository.updateProgress(id, DownloadState.DOWNLOADING, downloaded, totalBytes, target.absolutePath, expectedSha, speed, eta, null)
                        setForeground(downloadForegroundInfo(id, fileName, downloaded, totalBytes))
                        lastPublished = downloaded
                        lastSampleBytes = downloaded
                        lastSampleAt = now
                    }
                }
            }
        }
        val now = System.nanoTime()
        val elapsedSeconds = (now - lastSampleAt) / 1_000_000_000.0
        val speed = if (elapsedSeconds > 0.0) ((downloaded - lastSampleBytes) / max(elapsedSeconds, 0.001)).toLong() else 0L
        repository.updateProgress(id, DownloadState.DOWNLOADING, downloaded, totalBytes, target.absolutePath, expectedSha, speed, null, null)
        setForeground(downloadForegroundInfo(id, fileName, downloaded, totalBytes))
    }

    private fun parseContentRange(value: String): Triple<Long, Long, Long>? {
        val match = Regex("^bytes\\s+(\\d+)-(\\d+)/(\\d+)$", RegexOption.IGNORE_CASE).matchEntire(value.trim()) ?: return null
        val start = match.groupValues[1].toLongOrNull() ?: return null
        val end = match.groupValues[2].toLongOrNull() ?: return null
        val total = match.groupValues[3].toLongOrNull() ?: return null
        if (end < start || total <= end) return null
        return Triple(start, end, total)
    }

    private fun downloadForegroundInfo(downloadId: Long, fileName: String, downloadedBytes: Long, totalBytes: Long): ForegroundInfo {
        ensureDownloadChannel()
        val hasKnownTotal = totalBytes > 0
        val percent = if (hasKnownTotal) (downloadedBytes.coerceAtLeast(0) * 100 / totalBytes).toInt().coerceIn(0, 100) else 0
        val notification = NotificationCompat.Builder(applicationContext, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading $fileName")
            .setContentText(if (hasKnownTotal) "$percent% complete" else "Preparing download")
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(if (hasKnownTotal) 100 else 0, percent, !hasKnownTotal)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ForegroundInfo(notificationId(downloadId), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(notificationId(downloadId), notification)
    }

    private fun ensureDownloadChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(DOWNLOAD_CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = "GitHub release, artifact, image, and APK download progress"
            setShowBadge(false)
        })
    }

    private fun notificationId(downloadId: Long): Int = ((downloadId xor (downloadId ushr 32)).toInt() and Int.MAX_VALUE).coerceAtLeast(1)
    private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    companion object {
        const val KEY_ID = "download_id"
        const val KEY_URL = "download_url"
        const val KEY_NAME = "download_name"
        const val KEY_SHA256 = "download_sha256"
        const val KEY_PARTIAL_PATH = "download_partial_path"
        const val KEY_EXPECTED_PACKAGE = "download_expected_package"
        private const val DOWNLOAD_CHANNEL_ID = "github_rock_downloads"
        private const val PROGRESS_UPDATE_BYTES = 256 * 1024L
        private const val MAX_AUTOMATIC_RETRIES = 2
        fun workName(id: Long): String = "github-rock-download-$id"
    }
}