plugins {
    id("java-library")
    application
}

val nativePlatforms = listOf(
    "windows-x86_64",
    "windows-aarch64",
    "linux-x86_64",
    "linux-aarch64",
    "macos-x86_64",
    "macos-aarch64"
)

group = "com.rtm516.nethernettester"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
    maven("https://maven.lenni0451.net/snapshots")
}

dependencies {
    api(libs.gson)
    api(libs.methanol)
    api(libs.minecraftauth)
    api(libs.bundles.protocol)
    api(libs.netty.transport.nethernet)

    api(libs.webrtc)
    nativePlatforms.forEach { platform ->
        runtimeOnly(libs.webrtc) {
            artifact {
                classifier = platform
            }
        }
    }

    api(libs.terminalconsoleappender) {
        exclude("org.apache.logging.log4j")
        exclude("org.jline")
    }
    api(libs.bundles.jline)
    api(libs.bundles.log4j)
}

tasks.test {
    useJUnitPlatform()
}