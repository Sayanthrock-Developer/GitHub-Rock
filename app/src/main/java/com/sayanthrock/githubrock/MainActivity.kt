package com.sayanthrock.githubrock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sayanthrock.githubrock.navigation.GitHubRockRoot
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitHubRockTheme {
                GitHubRockRoot()
            }
        }
    }
}
