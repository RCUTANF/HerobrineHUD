plugins {
    kotlin("jvm") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
}

val modVersion = project.property("mod_version") as String
val mavenGroup = project.property("maven_group") as String

fun Project.stringPropertyOrNull(name: String): String? = findProperty(name)?.toString()

fun Project.currentMinecraftVersion(): String = (
    stringPropertyOrNull("debugVersion")
        ?: stringPropertyOrNull("defaultMcVersion")
        ?: stringPropertyOrNull("minecraft_version")
        ?: "unknown"
)

fun versionPropertySuffix(version: String): String {
    val parts = version.split('.')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (parts.isEmpty()) {
        throw GradleException("Invalid Minecraft version: $version")
    }

    return parts.joinToString("_")
}

fun resolveVersionedProperty(
    project: Project,
    baseName: String,
    version: String = project.currentMinecraftVersion()
): String {
    val versionedName = "${baseName}_${versionPropertySuffix(version)}"
    return project.stringPropertyOrNull(versionedName)
        ?: project.stringPropertyOrNull(baseName)
        ?: throw GradleException(
            "Missing property '$baseName' for Minecraft version '$version' (also tried '$versionedName')."
        )
}

fun mcVersionCode(version: String): Int {
    val parts = version.split('.').map { segment ->
        segment.trim().toIntOrNull()
            ?: throw GradleException("Invalid Minecraft version segment '$segment' in '$version'.")
    }

    if (parts.isEmpty()) {
        throw GradleException("Invalid Minecraft version: $version")
    }

    return parts.fold(0) { acc, part ->
        if (part !in 0..999) {
            throw GradleException("Minecraft version segment '$part' in '$version' is out of supported range 0..999.")
        }

        acc * 1_000 + part
    }
}

extra["currentMinecraftVersion"] = { project: Project -> project.currentMinecraftVersion() }
extra["resolveVersionedProperty"] = { project: Project, baseName: String, version: String ->
    resolveVersionedProperty(project, baseName, version)
}
extra["mcVersionCode"] = { version: String -> mcVersionCode(version) }
extra["versionPropertySuffix"] = { version: String -> versionPropertySuffix(version) }

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
val debugVersion = currentMinecraftVersion()

fun resolveFabricProject(version: String): String = if (version.startsWith("1.21.")) {
    ":fabric-remap"
} else {
    ":fabric"
}

fun resolveLoaderProject(loader: String, version: String): String = when (loader.lowercase()) {
    "fabric" -> resolveFabricProject(version)
    "neoforge" -> ":neoforge"
    else -> throw GradleException("Unsupported debugLoader: $loader")
}

tasks.register("runDebugClient") {
    group = "run"
    description = "Run debug client for the configured loader/version."
    dependsOn("${resolveLoaderProject(debugLoader, debugVersion)}:runClient")
    doFirst {
        logger.lifecycle("Debug target: loader=$debugLoader, version=$debugVersion")
    }
}

tasks.register("runDebugServer") {
    group = "run"
    description = "Run debug server for the configured loader/version."
    dependsOn("${resolveLoaderProject(debugLoader, debugVersion)}:runServer")
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
