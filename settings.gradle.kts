rootProject.name = "NetherNetTester"

include("bootstrap")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.opencollab.dev/main/")
        maven("https://maven.lenni0451.net/snapshots")
    }
}
