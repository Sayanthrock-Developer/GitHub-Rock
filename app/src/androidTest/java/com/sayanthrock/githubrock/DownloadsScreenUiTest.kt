package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.core.model.DownloadMirror
import com.sayanthrock.githubrock.ui.screens.DownloadCommandBar
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DownloadsScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun commandBarShowsSourceWithoutManualAddAction() {
        var changedSource = false

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                DownloadCommandBar(
                    selectedMirror = DownloadMirror.Direct,
                    onChangeMirror = { changedSource = true }
                )
            }
        }

        compose.onNodeWithText("Download source").assertIsDisplayed()
        compose.onNodeWithText("Direct GitHub").assertIsDisplayed().performClick()
        compose.onNodeWithText("Add download").assertDoesNotExist()
        compose.onNodeWithText("Download image or file").assertDoesNotExist()

        compose.runOnIdle {
            assertTrue(changedSource)
        }
    }
}
