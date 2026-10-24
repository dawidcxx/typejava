plugins {
    application
}

base {
    archivesName = "typejava-examples"
}

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass = "cc.kilonode.typejava.examples.Main"
    applicationName = "typejava-examples"
}

tasks.named("distZip") {
    enabled = false
}

tasks.named("distTar") {
    enabled = false
}
