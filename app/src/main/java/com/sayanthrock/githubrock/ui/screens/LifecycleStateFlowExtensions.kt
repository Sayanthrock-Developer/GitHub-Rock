package com.sayanthrock.githubrock.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle as lifecycleCollectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun <T> StateFlow<T>.collectAsStateWithLifecycle(): State<T> =
    lifecycleCollectAsStateWithLifecycle()
