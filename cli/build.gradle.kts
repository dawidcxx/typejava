import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar

plugins {
    application
}

base {
    archivesName = "typejava"
}

dependencies {
    implementation(project(":lib"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "cc.kilonode.typejava.cli.Main"
    applicationName = "typejava"
}

tasks.jar {
    manifest {
        attributes["Automatic-Module-Name"] = "cc.kilonode.typejava.cli"
    }
}

val cliJar = tasks.register<Jar>("cliJar") {
    group = "build"
    description = "Builds the self-contained TypeJava command-line jar."
    archiveClassifier = "all"

    manifest {
        attributes["Automatic-Module-Name"] = "cc.kilonode.typejava.cli"
        attributes["Main-Class"] = "cc.kilonode.typejava.cli.Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
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

tasks.named("distZip") {
    enabled = false
}

tasks.named("distTar") {
    enabled = false
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.test {
    workingDir = projectDir
}
