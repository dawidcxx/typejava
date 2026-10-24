import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    `java-library`
    `maven-publish`
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("version")
    .orElse(providers.gradleProperty("VERSION_NAME"))
    .get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(libs.asm)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.jar {
    manifest {
        attributes["Automatic-Module-Name"] = "cc.kilonode.typejava"
    }
}

val cliJar = tasks.register<Jar>("cliJar") {
    group = "build"
    description = "Builds the self-contained TypeJava command-line jar."
    archiveClassifier = "all"

    manifest {
        attributes["Automatic-Module-Name"] = "cc.kilonode.typejava.cli"
        attributes["Main-Class"] = "cc.kilonode.typejava.TypeJava"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get().map { dependency ->
            if (dependency.isDirectory) dependency else zipTree(dependency)
        }
    }) {
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/MANIFEST.MF",
            "module-info.class",
            "META-INF/versions/*/module-info.class",
        )
    }
}

tasks.named("assemble") {
    dependsOn(cliJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "TypeJava"
                description = "TypeScript-like scripting compiled to JVM bytecode with ASM."
                url = "https://github.com/dawidcxx/typejava"
                inceptionYear = "2026"

                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "dawidcxx"
                        name = "Dawid"
                        url = "https://github.com/dawidcxx/"
                    }
                }

                scm {
                    url = "https://github.com/dawidcxx/typejava"
                    connection = "scm:git:git://github.com/dawidcxx/typejava.git"
                    developerConnection = "scm:git:ssh://git@github.com/dawidcxx/typejava.git"
                }
            }
        }
    }

    repositories {
        val repository = providers.environmentVariable("GITHUB_REPOSITORY").orNull
        val token = providers.environmentVariable("GITHUB_TOKEN").orNull
        val snapshot = version.toString().endsWith("-SNAPSHOT")

        if (repository != null && token != null && !snapshot) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/$repository")
                credentials {
                    username = providers.environmentVariable("GITHUB_ACTOR").orNull ?: "github"
                    password = token
                }
            }
        }
    }
}
