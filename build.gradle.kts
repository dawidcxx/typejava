import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
}

val libraryGroup = providers.gradleProperty("GROUP").get()
val libraryVersion = providers.gradleProperty("version")
    .orElse(providers.gradleProperty("VERSION_NAME"))
    .get()

allprojects {
    group = libraryGroup
    version = libraryVersion
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release = 21
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        tasks.withType<AbstractArchiveTask>().configureEach {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
        }
    }
}

tasks.named("assemble") {
    dependsOn(":lib:assemble", ":cli:assemble", ":examples:assemble")
}

tasks.named("check") {
    dependsOn(":lib:check", ":cli:check", ":examples:check")
}

tasks.named("build") {
    description = "Builds all TypeJava modules."
    dependsOn("assemble", "check")
}
