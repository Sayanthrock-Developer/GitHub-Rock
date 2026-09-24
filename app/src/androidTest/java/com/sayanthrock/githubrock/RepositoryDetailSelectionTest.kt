package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.screens.REPOSITORY_DETAIL_SELECTABLE_TEST_TAG
import com.sayanthrock.githubrock.ui.screens.RepositoryDetailSelectableText
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class RepositoryDetailSelectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun repositoryOverviewKeepsNameDescriptionAndStatsSelectable() {
        val repository = GitHubRepositoryModel(
            id = 42,
            name = "GitHub-Rock",
            fullName = "Sayanthrock-Developer/GitHub-Rock",
            owner = Owner(login = "Sayanthrock-Developer"),
            description = "Native GitHub productivity for Android",
            language = "Kotlin",
            defaultBranch = "main",
            stars = 321,
            forks = 45,
            openIssues = 6
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                RepositoryDetailSelectableText(repository)
            }
        }

        compose.onNodeWithTag(REPOSITORY_DETAIL_SELECTABLE_TEST_TAG).assertIsDisplayed()
        compose.onNodeWithText("GitHub-Rock").assertIsDisplayed()
        compose.onNodeWithText("Sayanthrock-Developer/GitHub-Rock").assertIsDisplayed()
        compose.onNodeWithText("Native GitHub productivity for Android").assertIsDisplayed()
        compose.onNodeWithText("321 stars • 45 forks • 6 open issues").assertIsDisplayed()
    }
}
