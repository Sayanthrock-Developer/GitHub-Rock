package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.RepositoryCreationForm
import com.sayanthrock.githubrock.ui.screens.RepositoryCreationOperation
import com.sayanthrock.githubrock.ui.screens.repositoryCreationError
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RepositoryCreationFormTest {
    @Test
    fun validFormBuildsTrimmedApiRequest() {
        val form = RepositoryCreationForm(
            ownerLogin = "SayanthRock",
            name = "  GitHub-Rock-Native  ",
            description = "  Native Android repository tools  ",
            privateRepository = true,
            initializeReadme = true,
            gitignoreTemplate = "Android",
            licenseTemplate = "mit",
            defaultBranch = "develop"
        )

        assertNull(form.validationError())
        val request = form.toRequest()
        assertEquals("GitHub-Rock-Native", request.name)
        assertEquals("Native Android repository tools", request.description)
        assertEquals(true, request.privateRepository)
        assertEquals(true, request.initializeReadme)
        assertEquals("Android", request.gitignoreTemplate)
        assertEquals("mit", request.licenseTemplate)
    }

    @Test
    fun ownerAndRepositoryNameAreRequired() {
        assertEquals(
            "Choose an owner account.",
            RepositoryCreationForm(name = "native-repo").validationError()
        )
        assertEquals(
            "Repository name is required.",
            RepositoryCreationForm(ownerLogin = "SayanthRock").validationError()
        )
    }

    @Test
    fun repositoryNameRejectsUnsafeCharacters() {
        listOf("bad name", "bad/name", "bad\\name", "bad!name", ".", "..").forEach { name ->
            assertEquals(
                "Repository name can use letters, numbers, dots, hyphens, and underscores only.",
                RepositoryCreationForm(ownerLogin = "SayanthRock", name = name).validationError()
            )
        }
    }

    @Test
    fun templatesRequireRepositoryInitialization() {
        val form = RepositoryCreationForm(
            ownerLogin = "SayanthRock",
            name = "native-repo",
            initializeReadme = false,
            gitignoreTemplate = "Android"
        )

        assertEquals(
            "Initialize the repository before selecting a .gitignore or license template.",
            form.validationError()
        )
    }

    @Test
    fun initializedRepositoryRequiresSafeDefaultBranch() {
        val form = RepositoryCreationForm(
            ownerLogin = "SayanthRock",
            name = "native-repo",
            initializeReadme = true,
            defaultBranch = "feature bad"
        )

        assertEquals("Use a valid default branch name.", form.validationError())
    }

    @Test
    fun templatesAreRemovedFromRequestWhenInitializationIsOff() {
        val form = RepositoryCreationForm(
            ownerLogin = "SayanthRock",
            name = "native-repo",
            initializeReadme = false,
            gitignoreTemplate = "Android",
            licenseTemplate = "mit"
        )

        val request = form.toRequest()
        assertNull(request.gitignoreTemplate)
        assertNull(request.licenseTemplate)
    }

    @Test
    fun permissionFailuresNameTheRequiredScopeAndRole() {
        val forbidden = HttpException(
            Response.error<Unit>(
                403,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )

        assertEquals(
            "GitHub denied private repository creation. Reauthorize with the repo OAuth scope.",
            repositoryCreationError(forbidden, privateRepository = true, organization = false)
        )
        assertEquals(
            "GitHub denied repository creation. The OAuth token needs the repo scope and your organization role must allow repository creation.",
            repositoryCreationError(forbidden, privateRepository = false, organization = true)
        )
    }

    @Test
    fun optionLoadingNetworkFailureNamesTheFailedOperation() {
        assertEquals(
            "Network connection failed while loading repository creation options.",
            repositoryCreationError(
                error = IOException("offline"),
                privateRepository = false,
                organization = false,
                operation = RepositoryCreationOperation.LoadOptions
            )
        )
    }
}
