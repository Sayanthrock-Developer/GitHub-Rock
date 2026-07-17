package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.RepositoryReadmePolicy
import java.io.IOException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RepositoryReadmePolicyTest {
    @Test
    fun readableContentHasNoError() {
        assertNull(RepositoryReadmePolicy.errorMessage("# Project", null, "main"))
    }

    @Test
    fun missingReadmeIsDifferentFromTemporaryFailure() {
        assertEquals(
            "No readable README file was found on develop.",
            RepositoryReadmePolicy.errorMessage(null, null, "develop")
        )
        assertEquals(
            "README information is temporarily unavailable. Retry when the connection is stable.",
            RepositoryReadmePolicy.errorMessage(null, IOException("offline"), "main")
        )
    }

    @Test
    fun http404IsClassifiedAsMissing() {
        val error = HttpException(Response.error<String>(404, "missing".toResponseBody()))
        assertTrue(RepositoryReadmePolicy.isMissing(error))
        assertEquals(
            "No readable README file was found on main.",
            RepositoryReadmePolicy.errorMessage(null, error, "main")
        )
    }
}
