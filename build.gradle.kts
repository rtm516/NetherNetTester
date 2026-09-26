plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.rtm516"
version = "1.0-SNAPSHOT"

dependencies {
    api(libs.gson)
    api(libs.methanol)
    api(libs.minecraftauth)
    api(libs.bundles.protocol)

    api(libs.nethernet.transport)
    api(libs.libdatachannel)
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
