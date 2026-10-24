import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "typejava"

include("lib", "cli", "examples", "benchmarks")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
