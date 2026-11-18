import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.jvm.tasks.Jar
import xyz.jpenilla.runpaper.task.RunServer
import java.time.Duration

apply(from = "dependencies.gradle.kts")

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    java
}

val supportedJava = 25
val paperMinecraftVersion = project.property("minecraft_version") as String

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
    val tokens = mapOf(
        "cloud_kotlin_coroutines_annotations_version" to project.property("cloud_kotlin_coroutines_annotations_version"),
        "adventure_platform_bukkit_version" to project.property("adventure_platform_bukkit_version"),
        "adventure_minimessage_version" to project.property("adventure_minimessage_version"),
        "cloud_annotations_version" to project.property("cloud_annotations_version"),
        "cloud_paper_version" to project.property("cloud_paper_version"),
        "minecraft_version" to project.property("minecraft_version"),
        "kotlin_version" to project.property("kotlin_version"),
        "version" to project.property("plugin_version")
    )
    inputs.properties(tokens)
    filter<ReplaceTokens>("tokens" to tokens)
}

fun RunServer.configureWorldEditServer(additionalJvmArgs: List<String> = emptyList()) {
    minecraftVersion(paperMinecraftVersion)
    jvmArgs("-Dcom.mojang.eula.agree=true")
    jvmArgs(additionalJvmArgs)
    downloadPlugins {
        hangar("worldedit", "7.4.6")
    }
}

tasks {
    build {
        dependsOn(named("testServer"))
    }

    runServer {
        group = "run paper"
        description = "Runs a Paper server with the plugin installed for manual testing."
        runDirectory(layout.projectDirectory.dir("runs/server").asFile)
        configureWorldEditServer()
    }

    register<RunServer>("testServer") {
        group = "verification"
        description = "Runs the plugin against a real Paper server and verifies command behaviour."

        dependsOn(named<Jar>("jar"))
        pluginJars(named<Jar>("jar").flatMap { it.archiveFile })
        runDirectory(layout.projectDirectory.dir("runs/test-server").asFile)
        configureWorldEditServer(listOf("-Dworldeditaliases.testing=true"))
        timeout.set(Duration.ofMinutes(10))

        doFirst {
            layout.projectDirectory
                .file("runs/test-server/plugins/worldedit-aliases/test-results.txt")
                .asFile
                .delete()
        }

        doLast {
            val report = layout.projectDirectory
                .file("runs/test-server/plugins/worldedit-aliases/test-results.txt")
                .asFile

            if (!report.isFile) {
                throw GradleException(
                    "Integration test report was not created at ${report.path}. " +
                        "The test server likely failed to start or the plugin did not enable."
                )
            }

            if (report.readLines().firstOrNull() != "PASS") {
                throw GradleException("Integration tests failed:\n${report.readText()}")
            }

            logger.lifecycle("Integration tests passed (${report.path})")
        }
    }
}
