package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineResultTest {
    @Test(expected = CancellationException::class)
    fun cancellationIsRethrown() {
        runBlocking {
            runCatchingPreservingCancellation<String> {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun ordinaryFailureRemainsResultFailure() = runBlocking {
        val result = runCatchingPreservingCancellation<String> {
            throw IOException("offline")
        }

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
