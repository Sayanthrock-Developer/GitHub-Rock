package com.sayanthrock.githubrock

import org.junit.Assert.assertTrue
import org.junit.Test

class BranchSafetyTest {
    @Test fun branchNamesCannotContainShellSyntax() {
        assertTrue("feature/editor-v1".matches(Regex("^[A-Za-z0-9._/-]+$")))
        assertTrue(!"feature;curl-bad".matches(Regex("^[A-Za-z0-9._/-]+$")))
    }
}
