package com.sayanthrock.githubrock

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.core.model.DownloadMirror
import com.sayanthrock.githubrock.ui.screens.DownloadCommandBar
import com.sayanthrock.githubrock.ui.screens.EmptyDownloadsCard
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DownloadsScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun downloadsHeaderAndEmptyStateHaveNoManualAddAction() {
        var changedSource = false

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                Column {
                    DownloadCommandBar(
                        selectedMirror = DownloadMirror.Direct,
                        onChangeMirror = { changedSource = true }
                    )
                    EmptyDownloadsCard()
                }
            }
        }

        compose.onNodeWithText("Download source").assertIsDisplayed()
        compose.onNodeWithText("Direct GitHub").assertIsDisplayed().performClick()
        compose.onNodeWithText("No downloads yet").assertIsDisplayed()

        compose.runOnIdle {
            assertTrue(changedSource)
            assertEquals(
                0,
                compose.onAllNodesWithText("Add download").fetchSemanticsNodes().size
            )
            assertEquals(
                0,
                compose.onAllNodesWithText("Download image or file").fetchSemanticsNodes().size
            )
            assertEquals(
                0,
                compose.onAllNodesWithText("Add image or file").fetchSemanticsNodes().size
            )
        }
    }
}
