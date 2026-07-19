package com.sayanthrock.githubrock.core.util

enum class AndroidArtifactType(val gradleTask: String, val artifactGlob: String) {
    DebugApk("assembleDebug", "**/build/outputs/apk/debug/*.apk"),
    ReleaseApk("assembleRelease", "**/build/outputs/apk/release/*.apk")
}

data class AndroidProjectDetection(
    val isGradleProject: Boolean,
    val isAndroidProject: Boolean,
    val applicationModules: List<String>,
    val existingWorkflowPaths: List<String>
)

object AndroidProjectDetector {
    /**
     * Detects Gradle and Android project metadata from repository paths.
     *
     * @param paths Repository-relative file paths to inspect.
     * @return Detected project metadata, including Android modules and existing workflow paths.
     */
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

    /**
     * Generates a GitHub Actions workflow for building an installable Android APK.
     *
     * @param module The Android Gradle module to build.
     * @param artifact The APK type and corresponding Gradle task to use.
     * @param javaVersion The Java version for the workflow; supported values are 17 and 21.
     * @return The generated GitHub Actions workflow YAML.
     * @throws IllegalArgumentException If the module is invalid or the Java version is unsupported.
     */
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

    /**
     * Normalizes an Android module name to Gradle module notation.
     *
     * @param module The module name, using slash- or colon-separated segments.
     * @return The normalized module path with segments joined by colons.
     * @throws IllegalArgumentException If the module name is blank or contains an invalid segment.
     */
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
