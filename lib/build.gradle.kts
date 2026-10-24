plugins {
    `java-library`
    `maven-publish`
}

base {
    archivesName = "typejava"
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(libs.asm)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.jar {
    manifest {
        attributes["Automatic-Module-Name"] = "cc.kilonode.typejava"
    }
}

tasks.test {
    workingDir = projectDir
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "typejava"

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
