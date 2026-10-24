import org.gradle.api.tasks.JavaExec

plugins {
    java
}

base {
    archivesName = "typejava-benchmarks"
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.beanshell)
    implementation(libs.groovy)
    implementation(libs.jmh.core)
    implementation(libs.rhino)
    annotationProcessor(libs.jmh.generator.annprocess)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs the JMH log-classification benchmarks."
    mainClass = "org.openjdk.jmh.Main"
    classpath = sourceSets.main.get().runtimeClasspath
}
