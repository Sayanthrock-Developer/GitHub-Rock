package com.sayanthrock.githubrock.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Named-argument friendly overload for screens that describe the right-side label as a subtitle.
 */
@Composable
fun StandardSectionHeader(
    title: String,
    subtitle: CharSequence,
    modifier: Modifier = Modifier
) {
    StandardSectionHeader(
        title = title,
        supporting = subtitle.toString(),
        modifier = modifier
    )
}
