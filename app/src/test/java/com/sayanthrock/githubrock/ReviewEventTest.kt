package com.sayanthrock.githubrock

import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewEventTest {
    @Test fun supportedReviewEventsAreExplicit() {
        assertTrue(setOf("APPROVE", "REQUEST_CHANGES", "COMMENT").contains("APPROVE"))
        assertTrue(!setOf("APPROVE", "REQUEST_CHANGES", "COMMENT").contains("MERGE"))
    }
}
