package com.sayanthrock.githubrock.core.util

import retrofit2.HttpException

object RepositoryReadmePolicy {
    /**
 * Determines whether an error represents an HTTP 404 Not Found response.
 *
 * @param error The error to inspect.
 * @return `true` if the error is an HTTP 404 error, `false` otherwise.
 */
fun isMissing(error: Throwable): Boolean = error is HttpException && error.code() == 404

    /**
     * Determines the user-facing message for an unavailable README.
     *
     * @param readme The README content, when available.
     * @param failure The error encountered while fetching the README, if any.
     * @param branch The branch from which the README was requested.
     * @return `null` when readable content is available; otherwise, a message describing the failure.
     */
    fun errorMessage(readme: String?, failure: Throwable?, branch: String): String? = when {
        !readme.isNullOrBlank() -> null
        failure != null && !isMissing(failure) -> "README information is temporarily unavailable. Retry when the connection is stable."
        else -> "No readable README file was found on $branch."
    }
}
