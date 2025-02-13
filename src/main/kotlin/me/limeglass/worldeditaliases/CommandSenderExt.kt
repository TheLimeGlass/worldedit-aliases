package me.limeglass.worldeditaliases

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration

object CommandSenderExt {
    lateinit var adventure: BukkitAudiences
    lateinit var config: FileConfiguration
    lateinit var miniMessage: MiniMessage
}

fun CommandSender.sendTranslated(node: String, vararg parameters: Pair<String, Component>) {
    val translatedParams = parameters.map { (key, value) -> Placeholder.component(key.lowercase(), value) }
    CommandSenderExt.config.getString("messages.$node")?.let {
        CommandSenderExt.adventure.sender(this).sendMessage(
            CommandSenderExt.miniMessage.deserialize(it, TagResolver.resolver(translatedParams))
        )
    }
}

fun CommandSender.sendTranslatedList(node: String, vararg parameters: Pair<String, Component>) {
    val translatedParams = parameters.map { (key, value) -> Placeholder.component(key.lowercase(), value) }
    CommandSenderExt.config.getList("messages.$node")?.map { it as String }?.forEach {
        CommandSenderExt.adventure.sender(this).sendMessage(
            CommandSenderExt.miniMessage.deserialize(it, TagResolver.resolver(translatedParams))
        )
    }
}
