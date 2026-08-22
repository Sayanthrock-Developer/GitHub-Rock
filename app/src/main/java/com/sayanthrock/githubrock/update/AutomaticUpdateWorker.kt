package com.sayanthrock.githubrock.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sayanthrock.githubrock.R
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.util.ReleaseAssetClassifier
import com.sayanthrock.githubrock.core.util.ReleasePlatform
import com.sayanthrock.githubrock.data.local.DownloadDao
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.data.local.ManagedAppDao
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import com.sayanthrock.githubrock.download.DownloadWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Locale

@HiltWorker
class AutomaticUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: GitHubRepository,
    private val managedAppDao: ManagedAppDao,
    private val downloadDao: DownloadDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val apps = managedAppDao.autoUpdateApps()
        if (apps.isEmpty()) return Result.success()

        var hadTransientFailure = false
        apps.forEach { app ->
            try {
                val releases = repository.releases(app.repositoryOwner, app.repositoryName)
                val latest = releases.firstOrNull(::isEligibleRelease)
                if (latest == null) {
                    managedAppDao.recordCheck(app.packageName, null, now, null)
                    return@forEach
                }

                val updateAvailable = app.trackedReleaseTag != null &&
                    app.trackedReleaseTag != latest.tagName
                if (!updateAvailable) {
                    managedAppDao.recordCheck(app.packageName, latest.tagName, now, null)
                    return@forEach
                }

                val asset = selectApk(latest) ?: run {
                    managedAppDao.recordCheck(app.packageName, latest.tagName, now, null)
                    return@forEach
                }

                val existing = downloadDao.observeAllOnce().firstOrNull { download ->
                    download.status in ACTIVE_STATUSES &&
                        download.repositoryOwner == app.repositoryOwner &&
                        download.repositoryName == app.repositoryName &&
                        download.releaseTag == latest.tagName
                }
                if (existing != null) {
                    managedAppDao.recordCheck(app.packageName, latest.tagName, now, now)
                    return@forEach
                }

                val download = DownloadEntity(
                    fileName = asset.name,
                    sourceUrl = asset.downloadUrl,
                    status = "queued",
                    repositoryOwner = app.repositoryOwner,
                    repositoryName = app.repositoryName,
                    releaseTag = latest.tagName,
                    autoUpdate = true
                )
                val id = downloadDao.upsert(download)
                enqueueDownload(download.copy(id = id))
                managedAppDao.recordCheck(app.packageName, latest.tagName, now, now)
                notifyUpdateQueued(app.appName, latest)
            } catch (_: Exception) {
                hadTransientFailure = true
            }
        }

        return if (hadTransientFailure) Result.retry() else Result.success()
    }

    private suspend fun enqueueDownload(download: DownloadEntity) {
        val input = Data.Builder()
            .putLong(DownloadWorker.KEY_ID, download.id)
            .putString(DownloadWorker.KEY_URL, download.sourceUrl)
            .putString(DownloadWorker.KEY_NAME, download.fileName)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            DownloadWorker.workName(download.id),
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(input).build()
        )
    }

    private fun selectApk(release: Release) = release.assets
        .asSequence()
        .filter { it.name.lowercase(Locale.ROOT).endsWith(".apk") }
        .filter { ReleaseAssetClassifier.classify(it.name).isInstallablePackage }
        .toList()
        .let { ReleaseAssetClassifier.preferredAsset(it, ReleasePlatform.Android) }

    private fun isEligibleRelease(release: Release): Boolean =
        !release.draft && !release.prerelease && release.tagName.isNotBlank()

    private fun notifyUpdateQueued(appName: String, release: Release) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "App updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Automatic GitHub app update notifications" }
            )
        }
        manager.notify(
            notificationId(appName),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Update available")
                .setContentText("$appName ${release.tagName} is being downloaded")
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun notificationId(value: String): Int =
        value.hashCode().and(Int.MAX_VALUE).coerceAtLeast(1)

    private companion object {
        const val CHANNEL_ID = "github_rock_app_updates"
        val ACTIVE_STATUSES = setOf("queued", "downloading", "retrying")
    }
}

private suspend fun DownloadDao.observeAllOnce() = kotlinx.coroutines.flow.first(observeAll())
