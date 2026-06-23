import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.fabricmc.fabric-loom") version "1.16.2"
    id("maven-publish")
}

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

val mcVersion = currentMinecraftVersion()
val isActiveFabricProject = !mcVersion.startsWith("1.21.")
val effectiveMcVersion = if (isActiveFabricProject) mcVersion else "26.1"

logger.lifecycle("Fabric modern target mcVersion=$mcVersion effective=$effectiveMcVersion active=$isActiveFabricProject")

run {
    val baseName = project.property("archives_base_name") as String
    val loaderVersion = resolveVersionedProperty(project, "loader_version", effectiveMcVersion)
    val kotlinLoaderVersion = resolveVersionedProperty(project, "kotlin_loader_version", effectiveMcVersion)
    val fabricApiVersion = resolveVersionedProperty(project, "fabric_version", effectiveMcVersion)
    val minecraftDependency = project.stringPropertyOrNull("minecraft_dependency_${versionPropertySuffix(effectiveMcVersion)}")
        ?: effectiveMcVersion
    val fabricSourceRoot = "src"
    val mcVersionSourceRoot = "$fabricSourceRoot/mc$effectiveMcVersion"
    val mcMainSourceDir = "$mcVersionSourceRoot/main"
    val mcClientSourceDir = "$mcVersionSourceRoot/client"

    base {
        archivesName.set("$baseName-$effectiveMcVersion")
    }

    val targetJavaVersion = 25
    java {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }

    fun processMixinTemplate(input: String, versionCode: Int): String {
        data class IfState(val parent: Boolean, val condition: Boolean)
        val directiveRegex = Regex("""^\s*//\s*#(IF|ELSE|ENDIF)\b(.*)$""")
        val conditionRegex = Regex("""MC_VERSION\s*([<>=!]=?)\s*(\d+)""")
        val stack = ArrayDeque<IfState>()
        val output = StringBuilder()
        var include = true

        input.lineSequence().forEach { line ->
            val match = directiveRegex.matchEntire(line)
            if (match != null) {
                val directive = match.groupValues[1]
                val tail = match.groupValues[2].trim()
                when (directive) {
                    "IF" -> {
                        val condMatch = conditionRegex.find(tail)
                            ?: throw GradleException("Invalid mixin template condition: $line")
                        val op = condMatch.groupValues[1]
                        val value = condMatch.groupValues[2].toInt()
                        val condition = when (op) {
                            ">" -> versionCode > value
                            ">=" -> versionCode >= value
                            "<" -> versionCode < value
                            "<=" -> versionCode <= value
                            "==" -> versionCode == value
                            "!=" -> versionCode != value
                            else -> false
                        }
                        stack.addLast(IfState(include, condition))
                        include = include && condition
                    }

                    "ELSE" -> {
                        val state = stack.lastOrNull()
                            ?: throw GradleException("#ELSE without #IF in mixin template")
                        include = state.parent && !state.condition
                    }

                    "ENDIF" -> {
                        val state = stack.removeLastOrNull()
                            ?: throw GradleException("#ENDIF without #IF in mixin template")
                        include = state.parent
                    }
                }
            } else if (include) {
                output.appendLine(line)
            }
        }

        if (stack.isNotEmpty()) {
            throw GradleException("Unclosed #IF in mixin template")
        }

        return output.toString()
    }

    val mixinTemplateDir = file("$fabricSourceRoot/mixin-template")
    val mixinGeneratedDir = layout.buildDirectory.dir("generated/sources/mixinTemplates")

    val generateMixinTemplates = tasks.register("generateMixinTemplates") {
        inputs.dir(mixinTemplateDir)
        inputs.property("mcVersion", effectiveMcVersion)
        inputs.property("mcVersionCode", mcVersionCode(effectiveMcVersion))
        outputs.dir(mixinGeneratedDir)
        doLast {
            if (!mixinTemplateDir.exists()) return@doLast
            val outputDir = mixinGeneratedDir.get().asFile
            outputDir.deleteRecursively()
            val versionCode = mcVersionCode(effectiveMcVersion)
            fileTree(mixinTemplateDir).matching { include("**/*.template.java") }.forEach { templateFile ->
                val relativePath = templateFile.relativeTo(mixinTemplateDir).path
                    .removeSuffix(".template.java") + ".java"
                val targetFile = outputDir.resolve(relativePath)
                targetFile.parentFile.mkdirs()
                val processed = processMixinTemplate(templateFile.readText(), versionCode)
                targetFile.writeText(processed, Charsets.UTF_8)
            }
        }
    }

    sourceSets.named("main") {
        java.srcDir("$fabricSourceRoot/main/java")
        resources.srcDir("$fabricSourceRoot/main/resources")
        java.srcDir("$mcMainSourceDir/java")
        resources.srcDir("$mcMainSourceDir/resources")
        java.srcDir(mixinGeneratedDir)
    }

    kotlin {
        sourceSets.named("main") {
            kotlin.srcDir("$fabricSourceRoot/main/kotlin")
            kotlin.srcDir("$mcMainSourceDir/kotlin")
        }
    }

    loom {
        splitEnvironmentSourceSets()

        mods {
            register("herobrinehud") {
                sourceSet(sourceSets.getByName("main"))
                sourceSet(sourceSets.getByName("client"))
            }
        }

        runs {
            named("client") {
                runDir = "run/fabric-$effectiveMcVersion"
            }
            named("server") {
                runDir = "run/fabric-$effectiveMcVersion"
            }
        }
    }

    sourceSets.named("client") {
        java.srcDir("$fabricSourceRoot/client/java")
        resources.srcDir("$fabricSourceRoot/client/resources")
        java.srcDir("$mcClientSourceDir/java")
        resources.srcDir("$mcClientSourceDir/resources")
    }

    kotlin {
        sourceSets.named("client") {
            kotlin.srcDir("$fabricSourceRoot/client/kotlin")
            kotlin.srcDir("$mcClientSourceDir/kotlin")
        }
    }

    fabricApi {
        configureDataGeneration {
            client = true
        }
    }

    dependencies {
        minecraft("com.mojang:minecraft:$effectiveMcVersion")

        implementation("net.fabricmc:fabric-loader:$loaderVersion")
        implementation("net.fabricmc:fabric-language-kotlin:$kotlinLoaderVersion")
        implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
        implementation(project(":common"))
    }

    tasks.processResources {
        inputs.property("version", project.version)
        inputs.property("minecraft_version", effectiveMcVersion)
        inputs.property("minecraft_dependency", minecraftDependency)
        inputs.property("loader_version", loaderVersion)
        inputs.property("kotlin_loader_version", kotlinLoaderVersion)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to project.version,
                    "minecraft_version" to effectiveMcVersion,
                    "minecraft_dependency" to minecraftDependency,
                    "loader_version" to loaderVersion,
                    "kotlin_loader_version" to kotlinLoaderVersion
                )
            )
        }
    }

    tasks.named<ProcessResources>("processClientResources") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(targetJavaVersion)
        dependsOn(generateMixinTemplates)
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
        dependsOn(generateMixinTemplates)
    }

    tasks.jar {
        exclude("**/.gitkeep")
        from(rootProject.file("LICENSE.txt")) {
            rename { "${it}_${project.base.archivesName.get()}" }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = "${project.property("archives_base_name") as String}-$effectiveMcVersion"
                from(components["java"])
            }
        }

        repositories {
        }
    }

    if (!isActiveFabricProject) {
        tasks.configureEach {
            enabled = false
        }
    }
}
