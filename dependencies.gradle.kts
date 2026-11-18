import org.gradle.kotlin.dsl.extra

// Done to allow dependabot scanning

val minecraftVersion = "26.3"

extra.set("plugin_version", "1.5")
extra.set("minecraft_version", minecraftVersion)
extra.set("paper_version", "$minecraftVersion.build.+")
extra.set("worldedit_version", "7.4.6")
extra.set("adventure_minimessage_version", "4.26.1")
extra.set("adventure_platform_bukkit_version", "4.4.1")
extra.set("cloud_kotlin_coroutines_annotations_version", "2.1.0")
extra.set("cloud_paper_version", "2.0.1")
extra.set("cloud_annotations_version", "2.1.0")
extra.set("kotlin_version", "2.4.10")
