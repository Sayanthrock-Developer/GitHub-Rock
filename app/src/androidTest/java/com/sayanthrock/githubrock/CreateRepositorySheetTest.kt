package com.sayanthrock.githubrock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.core.model.RepositoryCreationForm
import com.sayanthrock.githubrock.core.model.RepositoryLicenseTemplate
import com.sayanthrock.githubrock.core.model.RepositoryOwnerOption
import com.sayanthrock.githubrock.core.model.RepositoryOwnerType
import com.sayanthrock.githubrock.ui.screens.CreateRepositoryFormContent
import com.sayanthrock.githubrock.ui.screens.CreateRepositoryState
import com.sayanthrock.githubrock.ui.screens.RepositoriesScreen
import org.junit.Rule
import org.junit.Test

class CreateRepositorySheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun formShowsNativeRepositoryOptionsAndPermissionError() {
        val permissionError =
            "GitHub denied private repository creation. Reauthorize with the repo OAuth scope."
        composeRule.setContent {
            MaterialTheme {
                CreateRepositoryFormContent(
                    state = CreateRepositoryState(
                        owners = listOf(
                            RepositoryOwnerOption(
                                login = "SayanthRock",
                                avatarUrl = "",
                                type = RepositoryOwnerType.User
                            )
                        ),
                        gitignoreTemplates = listOf("Android", "Kotlin"),
                        licenses = listOf(RepositoryLicenseTemplate("mit", "MIT License", "MIT")),
                        optionsLoaded = true,
                        error = permissionError
                    ),
                    form = RepositoryCreationForm(
                        ownerLogin = "SayanthRock",
                        name = "GitHub-Rock-Native",
                        privateRepository = true,
                        initializeReadme = true
                    ),
                    onFormChange = {},
                    onCreate = {},
                    onCancel = {},
                    onOpenCreated = {}
                )
            }
        }

        composeRule.onNodeWithText("Create repository").assertExists()
        composeRule.onNodeWithText("SayanthRock").assertExists()
        composeRule.onNodeWithText("Repository name").assertExists()
        composeRule.onNodeWithText("Private repository").assertExists()
        composeRule.onNodeWithText("Initialize README").assertExists()
        composeRule.onNodeWithText(".gitignore template").assertExists()
        composeRule.onNodeWithText("License template").assertExists()
        composeRule.onNodeWithText("Default branch").assertExists()
        composeRule.onNodeWithText(permissionError).performScrollTo().assertExists()
        composeRule.onNodeWithText("Create").performScrollTo().assertIsEnabled()
    }

    @Test
    fun loadingStatePreventsRepositorySubmission() {
        composeRule.setContent {
            MaterialTheme {
                CreateRepositoryFormContent(
                    state = CreateRepositoryState(loadingOptions = true),
                    form = RepositoryCreationForm(),
                    onFormChange = {},
                    onCreate = {},
                    onCancel = {},
                    onOpenCreated = {}
                )
            }
        }

        composeRule.onNodeWithText("Loading owner accounts and GitHub templates…")
            .assertTextContains("Loading owner accounts")
        composeRule.onNodeWithText("Create").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun readOnlyModeDoesNotExposeRepositoryCreation() {
        composeRule.setContent {
            MaterialTheme {
                RepositoriesScreen(
                    repositories = emptyList(),
                    loading = false,
                    onSearch = {},
                    creationEnabled = false,
                    onOpen = {}
                )
            }
        }

        composeRule.onNodeWithText("Repositories").assertExists()
        composeRule.onNodeWithText("New").assertDoesNotExist()
    }
}
