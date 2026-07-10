package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.AndroidArtifactType
import com.sayanthrock.githubrock.core.util.AndroidProjectDetector
import com.sayanthrock.githubrock.core.util.AndroidWorkflowGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBuildWorkflowTest {
    @Test fun detectsApplicationModules() {
        val result = AndroidProjectDetector.detect(listOf("gradlew", "settings.gradle.kts", "app/src/main/AndroidManifest.xml"))
        assertTrue(result.isAndroidProject)
        assertEquals(listOf("app"), result.applicationModules)
    }

    @Test fun generatorUsesFixedTaskMapping() {
        val yaml = AndroidWorkflowGenerator.generate("app", AndroidArtifactType.ReleaseAab)
        assertTrue(yaml.contains("./gradlew :app:bundleRelease"))
        assertTrue(yaml.contains("**/build/outputs/bundle/release/*.aab"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShellSyntaxInModuleName() {
        AndroidWorkflowGenerator.generate("app; curl bad", AndroidArtifactType.DebugApk)
    }
}

