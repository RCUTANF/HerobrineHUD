pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        gradlePluginPortal()
    }
}

rootProject.name = "HerobrineHUD-old"

include("common")
include("fabric")
include("neoforge")

