import org.apache.tools.ant.filters.ReplaceTokens

apply(from = "dependencies.gradle.kts")

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    java
}

val supportedJava = 25

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(supportedJava))
    }
}

kotlin {
    jvmToolchain(supportedJava)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-source", "$supportedJava", "-target", "$supportedJava"))
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://maven.enginehub.org/repo/")
}

dependencies {
    implementation("io.papermc.paper:paper-api:${project.property("paper_version")}")
    implementation("com.sk89q.worldedit:worldedit-bukkit:${project.property("worldedit_version")}") {
        exclude(module = "bstats-bukkit")
    }
    implementation("org.incendo:cloud-kotlin-coroutines-annotations:${project.property("cloud_kotlin_coroutines_annotations_version")}")
    implementation("net.kyori:adventure-platform-bukkit:${project.property("adventure_platform_bukkit_version")}")
    implementation("net.kyori:adventure-text-minimessage:${project.property("adventure_minimessage_version")}")
    implementation("org.incendo:cloud-annotations:${project.property("cloud_annotations_version")}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.property("kotlin_version")}")
    implementation("org.incendo:cloud-paper:${project.property("cloud_paper_version")}")
}

tasks.processResources {
    filter<ReplaceTokens>("tokens" to mapOf(
        "cloud_kotlin_coroutines_annotations_version" to project.property("cloud_kotlin_coroutines_annotations_version"),
        "adventure_platform_bukkit_version" to project.property("adventure_platform_bukkit_version"),
        "adventure_minimessage_version" to project.property("adventure_minimessage_version"),
        "cloud_annotations_version" to project.property("cloud_annotations_version"),
        "cloud_paper_version" to project.property("cloud_paper_version"),
        "kotlin_version" to project.property("kotlin_version"),
        "version" to project.property("plugin_version")
    ))
}
