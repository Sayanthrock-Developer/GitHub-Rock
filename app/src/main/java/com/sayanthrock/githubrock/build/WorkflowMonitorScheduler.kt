package com.sayanthrock.githubrock.build

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowMonitorScheduler @Inject constructor(
    @ApplicationContext context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun monitorDispatch(
        owner: String,
        repo: String,
        workflowId: Long,
        ref: String,
        knownRunIds: Set<Long>
    ) {
        val monitorKey = monitorKey(owner, repo, workflowId, ref)
        val request = OneTimeWorkRequestBuilder<WorkflowMonitorWorker>()
            .setInputData(
                Data.Builder()
                    .putString(WorkflowMonitorWorker.KEY_OWNER, owner)
                    .putString(WorkflowMonitorWorker.KEY_REPO, repo)
                    .putLong(WorkflowMonitorWorker.KEY_WORKFLOW_ID, workflowId)
                    .putString(WorkflowMonitorWorker.KEY_REF, ref)
                    .putString(WorkflowMonitorWorker.KEY_MONITOR, monitorKey)
                    .putLongArray(WorkflowMonitorWorker.KEY_KNOWN_RUN_IDS, knownRunIds.toLongArray())
                    .build()
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork("$TAG-$monitorKey", ExistingWorkPolicy.KEEP, request)
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG)
    }

    private fun monitorKey(owner: String, repo: String, workflowId: Long, ref: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$owner/$repo|$workflowId|$ref".toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private companion object {
        const val TAG = "workflow-monitor"
    }
}
