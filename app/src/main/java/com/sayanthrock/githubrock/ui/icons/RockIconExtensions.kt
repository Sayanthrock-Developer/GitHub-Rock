package com.sayanthrock.githubrock.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Compatibility bridge for call sites that import the historical top-level
 * vector extension. The canonical implementation lives on RockIcon itself.
 */
fun RockIcon.vector(selected: Boolean = false): ImageVector = this.vector(selected)
