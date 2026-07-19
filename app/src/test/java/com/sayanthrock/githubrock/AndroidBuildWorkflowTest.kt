package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.AndroidArtifactType
import com.sayanthrock.githubrock.core.util.AndroidProjectDetector
import com.sayanthrock.githubrock.core.util.AndroidWorkflowGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBuildWorkflowTest {
    @Test
    fun detectsApplicationModules() {
        val result = AndroidProjectDetector.detect(
            listOf("gradlew", "settings.gradle.kts", "app/src/main/AndroidManifest.xml")
        )

        assertTrue(result.isAndroidProject)
        assertEquals(listOf("app"), result.applicationModules)
    }

    @Test
    fun detectsNestedApplicationModules() {
        val result = AndroidProjectDetector.detect(
            listOf("settings.gradle.kts", "features/mobile/src/main/AndroidManifest.xml")
        )

        assertTrue(result.isAndroidProject)
        assertEquals(listOf("features/mobile"), result.applicationModules)
    }

    @Test
    fun generatorUsesFixedApkTaskMappingAndRepairsIncompleteWrapper() {
        val yaml = AndroidWorkflowGenerator.generate("app", AndroidArtifactType.ReleaseApk)

        assertTrue(yaml.contains("gradle-version: '8.13'"))
        assertTrue(yaml.contains("[ ! -f ./gradlew ] ||"))
        assertTrue(yaml.contains("[ ! -f gradle/wrapper/gradle-wrapper.jar ] ||"))
        assertTrue(yaml.contains("[ ! -f gradle/wrapper/gradle-wrapper.properties ]; then"))
        assertTrue(yaml.contains("gradle wrapper --gradle-version 8.13"))
        assertTrue(yaml.contains("./gradlew :app:assembleRelease"))
        assertTrue(yaml.contains("**/build/outputs/apk/release/*.apk"))
    }

    @Test
    fun generatorConvertsNestedModulePathsToGradleProjectPaths() {
        val yaml = AndroidWorkflowGenerator.generate("features/mobile", AndroidArtifactType.DebugApk)

        assertTrue(yaml.contains("./gradlew :features:mobile:assembleDebug"))
        assertTrue(yaml.contains("name: features-mobile-debugapk"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShellSyntaxInModuleName() {
        AndroidWorkflowGenerator.generate("app; curl bad", AndroidArtifactType.DebugApk)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTraversalInModuleName() {
        AndroidWorkflowGenerator.generate("features/../app", AndroidArtifactType.DebugApk)
    }
}
