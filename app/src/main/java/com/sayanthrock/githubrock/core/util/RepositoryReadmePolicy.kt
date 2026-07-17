package com.sayanthrock.githubrock.core.util

import retrofit2.HttpException

object RepositoryReadmePolicy {
    fun isMissing(error: Throwable): Boolean = error is HttpException && error.code() == 404

    fun errorMessage(readme: String?, failure: Throwable?, branch: String): String? = when {
        !readme.isNullOrBlank() -> null
        failure != null && !isMissing(failure) -> "README information is temporarily unavailable. Retry when the connection is stable."
        else -> "No readable README file was found on $branch."
    }
}
