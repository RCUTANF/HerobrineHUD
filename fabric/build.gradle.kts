import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.fabricmc.fabric-loom") version "1.15.5"
    id("maven-publish")
}

val baseName = project.property("archives_base_name") as String
val mcVersion = (
    findProperty("debugVersion")
        ?: findProperty("defaultMcVersion")
        ?: project.property("minecraft_version")
).toString()
val mcSourceDir = "src/mc$mcVersion"

base {
    // Append the targeted Minecraft version to the archive name so generated jars
    // include the minecraft version (e.g. mymod-1.20.4.jar).
    archivesName.set("$baseName-$mcVersion")
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

fun mcVersionCode(version: String): Int {
    val parts = version.split('.').mapNotNull { it.toIntOrNull() }
    return when (parts.size) {
        2 -> parts[0] * 100 + parts[1] * 10
        3 -> parts[0] * 1000 + parts[1] * 10 + parts[2]
        else -> parts.joinToString("").toIntOrNull() ?: 0
    }
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

val mixinTemplateDir = file("src/mixin-template")
val mixinGeneratedDir = layout.buildDirectory.dir("generated/sources/mixinTemplates")

val generateMixinTemplates = tasks.register("generateMixinTemplates") {
    inputs.dir(mixinTemplateDir)
    inputs.property("mcVersion", mcVersion)
    inputs.property("mcVersionCode", mcVersionCode(mcVersion))
    outputs.dir(mixinGeneratedDir)
    doLast {
        if (!mixinTemplateDir.exists()) return@doLast
        val outputDir = mixinGeneratedDir.get().asFile
        outputDir.deleteRecursively()
        val versionCode = mcVersionCode(mcVersion)
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

sourceSets {
    named("main") {
        java.srcDir("$mcSourceDir/java")
        resources.srcDir("$mcSourceDir/resources")
        java.srcDir(mixinGeneratedDir)
    }
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("$mcSourceDir/kotlin")
        }
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("herobrinehud") {
            sourceSet("main")
            sourceSet("client")
        }
    }

    runs {
        named("client") {
            runDir = "run/fabric-$mcVersion"
        }
        named("server") {
            runDir = "run/fabric-$mcVersion"
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:$mcVersion")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    implementation(project(":common"))
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", mcVersion)
    inputs.property("loader_version", project.property("loader_version"))
    inputs.property("kotlin_loader_version", project.property("kotlin_loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "minecraft_version" to mcVersion,
                "loader_version" to project.property("loader_version"),
                "kotlin_loader_version" to project.property("kotlin_loader_version")
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
    dependsOn(generateMixinTemplates)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
    dependsOn(generateMixinTemplates)
}

tasks.jar {
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            // Make the published artifactId include the Minecraft version to match the
            // produced archive name (e.g. herobrinehud-26.1)
            artifactId = "${project.property("archives_base_name") as String}-$mcVersion"
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information about how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}


