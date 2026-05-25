plugins {
    kotlin("jvm") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
}

val modVersion = project.property("mod_version") as String
val mavenGroup = project.property("maven_group") as String

allprojects {
    version = modVersion
    group = mavenGroup
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
    }
}

val debugLoader = (findProperty("debugLoader") ?: "fabric").toString()
val debugVersion = (
    findProperty("debugVersion")
        ?: findProperty("defaultMcVersion")
        ?: findProperty("minecraft_version")
        ?: "unknown"
).toString()

fun resolveLoaderProject(loader: String): String = when (loader.lowercase()) {
    "fabric" -> ":fabric"
    "neoforge" -> ":neoforge"
    else -> throw GradleException("Unsupported debugLoader: $loader")
}

tasks.register("runDebugClient") {
    group = "run"
    description = "Run debug client for the configured loader/version."
    dependsOn("${resolveLoaderProject(debugLoader)}:runClient")
    doFirst {
        logger.lifecycle("Debug target: loader=$debugLoader, version=$debugVersion")
    }
}

tasks.register("runDebugServer") {
    group = "run"
    description = "Run debug server for the configured loader/version."
    dependsOn("${resolveLoaderProject(debugLoader)}:runServer")
    doFirst {
        logger.lifecycle("Debug target: loader=$debugLoader, version=$debugVersion")
    }
}

tasks.register("runDebugClientFabric") {
    group = "run"
    description = "Run debug client for Fabric."
    dependsOn(":fabric:runClient")
}

tasks.register("runDebugServerFabric") {
    group = "run"
    description = "Run debug server for Fabric."
    dependsOn(":fabric:runServer")
}

tasks.register("runDebugClientNeoForge") {
    group = "run"
    description = "Run debug client for NeoForge."
    dependsOn(":neoforge:runClient")
}

tasks.register("runDebugServerNeoForge") {
    group = "run"
    description = "Run debug server for NeoForge."
    dependsOn(":neoforge:runServer")
}
