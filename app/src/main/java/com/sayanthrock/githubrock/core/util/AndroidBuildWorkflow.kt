package com.sayanthrock.githubrock.core.util

enum class AndroidArtifactType(val gradleTask: String, val artifactGlob: String) {
    DebugApk("assembleDebug", "**/build/outputs/apk/debug/*.apk"),
    ReleaseApk("assembleRelease", "**/build/outputs/apk/release/*.apk"),
    ReleaseAab("bundleRelease", "**/build/outputs/bundle/release/*.aab")
}

data class AndroidProjectDetection(
    val isGradleProject: Boolean,
    val isAndroidProject: Boolean,
    val applicationModules: List<String>,
    val existingWorkflowPaths: List<String>
)

object AndroidProjectDetector {
    fun detect(paths: Collection<String>): AndroidProjectDetection {
        val normalized = paths.map { it.replace('\\', '/').trimStart('/') }
        val gradle = normalized.any { it == "gradlew" || it == "settings.gradle" || it == "settings.gradle.kts" }
        val manifests = normalized.filter { it.endsWith("/src/main/AndroidManifest.xml") }
        val modules = manifests
            .map { it.substringBefore("/src/main/AndroidManifest.xml") }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
        return AndroidProjectDetection(
            isGradleProject = gradle,
            isAndroidProject = gradle && modules.isNotEmpty(),
            applicationModules = modules,
            existingWorkflowPaths = normalized.filter {
                it.startsWith(".github/workflows/") && (it.endsWith(".yml") || it.endsWith(".yaml"))
            }
        )
    }
}

object AndroidWorkflowGenerator {
    private const val DEFAULT_GRADLE_VERSION = "8.13"
    private val safeModuleSegment = Regex("^[A-Za-z0-9_.-]+$")

    fun generate(module: String, artifact: AndroidArtifactType, javaVersion: Int = 17): String {
        val gradleModule = normalizeModule(module)
        require(javaVersion in setOf(17, 21)) { "Unsupported Java version" }
        val task = ":$gradleModule:${artifact.gradleTask}"
        val artifactName = gradleModule.replace(':', '-')
        return """
            name: Android Build

            on:
              workflow_dispatch:

            permissions:
              contents: read

            jobs:
              build:
                runs-on: ubuntu-latest
                timeout-minutes: 45
                steps:
                  - uses: actions/checkout@v4
                  - uses: actions/setup-java@v4
                    with:
                      distribution: temurin
                      java-version: '$javaVersion'
                  - uses: gradle/actions/setup-gradle@v4
                    with:
                      gradle-version: '$DEFAULT_GRADLE_VERSION'
                  - name: Prepare Gradle wrapper
                    shell: bash
                    run: |
                      set -euo pipefail
                      if [ ! -f ./gradlew ] || \
                         [ ! -f gradle/wrapper/gradle-wrapper.jar ] || \
                         [ ! -f gradle/wrapper/gradle-wrapper.properties ]; then
                        gradle wrapper --gradle-version $DEFAULT_GRADLE_VERSION
                      fi
                      chmod +x ./gradlew
                  - name: Build ${artifact.name}
                    run: ./gradlew $task --stacktrace
                  - uses: actions/upload-artifact@v4
                    with:
                      name: ${artifactName}-${artifact.name.lowercase()}
                      path: ${artifact.artifactGlob}
                      if-no-files-found: error
        """.trimIndent() + "\n"
    }

    private fun normalizeModule(module: String): String {
        val trimmed = module.trim().trim(':', '/')
        require(trimmed.isNotBlank()) { "Android module name is required" }
        val segments = trimmed.split('/', ':')
        require(
            segments.all { segment ->
                segment.isNotBlank() &&
                    segment != "." &&
                    segment != ".." &&
                    safeModuleSegment.matches(segment)
            }
        ) { "Unsafe Android module name" }
        return segments.joinToString(":")
    }
}
