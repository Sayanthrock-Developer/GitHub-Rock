package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sayanthrock.githubrock.ui.screens.DownloadListFilter
import com.sayanthrock.githubrock.ui.screens.DownloadsRedesignContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class DownloadsScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun downloadsOpenDirectlyToFiltersWithoutSummaryOrSourceCards() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                DownloadsRedesignContent(
                    downloads = emptyList(),
                    selectedFilter = DownloadListFilter.All,
                    onSelectFilter = {},
                    onPrimaryAction = {},
                    onOpenActions = {}
                )
            }
        }

        compose.onNodeWithText("All").assertIsDisplayed()
        compose.onNodeWithText("No downloads yet").assertIsDisplayed()

        compose.onNodeWithText("Downloads").assertDoesNotExist()
        compose.onNodeWithText("Applications, build artifacts, images, and files in one clean workspace").assertDoesNotExist()
        compose.onNodeWithText("Download source").assertDoesNotExist()
        compose.onNodeWithText("Direct GitHub").assertDoesNotExist()
        compose.onNodeWithText("Change").assertDoesNotExist()
    }
}
