plugins {
    application
}

group = "com.rtm516"
version = rootProject.version

dependencies {
    implementation(rootProject)

    implementation(libs.terminalconsoleappender) {
        exclude("org.apache.logging.log4j")
        exclude("org.jline")
    }
    implementation(libs.bundles.jline)
    implementation(libs.bundles.log4j)
}

application {
    mainClass = "com.rtm516.nethernettester.bootstrap.Main"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("run").apply { mkdirs() }
}
