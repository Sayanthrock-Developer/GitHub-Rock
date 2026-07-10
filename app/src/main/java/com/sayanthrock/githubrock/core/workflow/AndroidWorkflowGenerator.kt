package com.sayanthrock.githubrock.core.workflow

enum class AndroidArtifactType(val label: String, val task: String, val outputPattern: String) {
    DEBUG_APK("Debug APK", "assembleDebug", "**/build/outputs/apk/debug/*.apk"),
    RELEASE_APK("Release APK", "assembleRelease", "**/build/outputs/apk/release/*.apk"),
    RELEASE_AAB("Release AAB", "bundleRelease", "**/build/outputs/bundle/release/*.aab")
}

object AndroidWorkflowGenerator {
    private val modulePattern = Regex("[A-Za-z0-9_-]+(?::[A-Za-z0-9_-]+)*")

    fun generate(module: String, artifactType: AndroidArtifactType): String {
        val normalizedModule = module.trim().removePrefix(":")
        require(modulePattern.matches(normalizedModule)) {
            "Module must contain only letters, numbers, underscores, hyphens, and colon separators."
        }
        val taskPath = ":$normalizedModule:${artifactType.task}"
        return """
            name: Android Build

            on:
              workflow_dispatch:
                inputs:
                  git_ref:
                    description: Branch or tag to build
                    required: false
                    default: ''

            permissions:
              contents: read

            jobs:
              build:
                runs-on: ubuntu-latest
                timeout-minutes: 45
                steps:
                  - name: Checkout selected ref
                    uses: actions/checkout@v4
                    with:
                      ref: ${'$'}{{ inputs.git_ref || github.ref }}

                  - name: Set up Java 17
                    uses: actions/setup-java@v4
                    with:
                      distribution: temurin
                      java-version: '17'
                      cache: gradle

                  - name: Validate Gradle wrapper
                    uses: gradle/actions/wrapper-validation@v4

                  - name: Make wrapper executable
                    run: chmod +x ./gradlew

                  - name: Build ${artifactType.label}
                    run: ./gradlew $taskPath --stacktrace

                  - name: Upload artifact
                    uses: actions/upload-artifact@v4
                    with:
                      name: ${normalizedModule.replace(':', '-')}-${artifactType.name.lowercase()}
                      path: ${artifactType.outputPattern}
                      if-no-files-found: error
                      retention-days: 14
        """.trimIndent()
    }
}
