import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val targetJavaVersion = 25
val mcVersion = (
    findProperty("debugVersion")
        ?: findProperty("defaultMcVersion")
        ?: "unknown"
).toString()
val mcSourceDir = "src/mc$mcVersion"
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

sourceSets {
    named("main") {
        java.srcDir("$mcSourceDir/java")
        resources.srcDir("$mcSourceDir/resources")
    }
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.srcDir("$mcSourceDir/kotlin")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.property("kotlinx_serialization_json_version")}")
}


