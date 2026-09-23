package com.sayanthrock.githubrock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sayanthrock.githubrock.build.BuildNotificationManager
import dagger.hilt.android.HiltAndroidApp
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class GitHubRockApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var buildNotifications: BuildNotificationManager
    @Inject lateinit var githubClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        buildNotifications.createChannel()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(githubClient.newBuilder().addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "GitHub-Rock/1.0 (Android; +https://github.com/Sayanthrock-Developer/GitHub-Rock)")
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                            .build()
                    )
                }.build())
                .components { add(SvgDecoder.Factory()) }
                .build()
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.INFO else android.util.Log.WARN)
            .build()
}
