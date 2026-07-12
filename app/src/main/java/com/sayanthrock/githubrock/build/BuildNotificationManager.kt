package com.sayanthrock.githubrock.build

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sayanthrock.githubrock.MainActivity
import com.sayanthrock.githubrock.R
import com.sayanthrock.githubrock.core.model.WorkflowRun
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GitHub Actions builds",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Completion status for Android builds selected in GitHub Rock"
            }
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showCompletion(owner: String, repo: String, run: WorkflowRun, completion: WorkflowCompletion) {
        if (!canNotify()) return
        createChannel()
        val ref = run.headBranch?.takeIf(String::isNotBlank) ?: "unknown ref"
        val content = "$owner/$repo • $ref • run ${run.id}"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(completion.title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(buildIntent(owner, repo, run.id))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(run.id.notificationId(), notification)
    }

    @SuppressLint("MissingPermission")
    fun showMonitoringStopped(owner: String, repo: String) {
        if (!canNotify()) return
        createChannel()
        val content = "Open GitHub Rock to refresh $owner/$repo."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Build monitoring stopped")
            .setContentText(content)
            .setContentIntent(buildRepositoryIntent(owner, repo))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify("$owner/$repo".hashCode(), notification)
    }

    private fun canNotify(): Boolean =
        manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun buildIntent(owner: String, repo: String, runId: Long): PendingIntent {
        val uri = Uri.Builder()
            .scheme("githubrock")
            .authority("build")
            .appendPath(owner)
            .appendPath(repo)
            .appendPath(runId.toString())
            .build()
        return pendingIntent(uri, runId.notificationId())
    }

    private fun buildRepositoryIntent(owner: String, repo: String): PendingIntent {
        val uri = Uri.Builder()
            .scheme("githubrock")
            .authority("repo")
            .appendPath(owner)
            .appendPath(repo)
            .build()
        return pendingIntent(uri, "$owner/$repo".hashCode())
    }

    private fun pendingIntent(uri: Uri, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun Long.notificationId(): Int = (this xor (this ushr 32)).toInt()

    private companion object {
        const val CHANNEL_ID = "github_actions_builds"
    }
}
