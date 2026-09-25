plugins {
    id("java-library")
    application
    id("maven-publish")
}

group = "com.rtm516"
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

    api(libs.nethernet.transport)
    api(libs.libdatachannel)

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "rtm516"
            url = uri(
                if (version.toString().endsWith("-SNAPSHOT"))
                    "https://repo.rtm516.co.uk/snapshots"
                else
                    "https://repo.rtm516.co.uk/releases"
            )

            credentials {
                username = project.findProperty("rtm516Username") as String?
                password = project.findProperty("rtm516Password") as String?
            }
        }
    }
}
