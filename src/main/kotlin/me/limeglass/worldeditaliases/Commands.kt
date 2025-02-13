package me.limeglass.worldeditaliases

import com.sk89q.worldedit.command.util.SuggestionHelper
import com.sk89q.worldedit.world.block.BlockType
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.paper.util.sender.Source

class Commands(
    private val aliases: MutableMap<String, String>,
    private val instance: WorldEditAliases
) {

    private companion object {
        private const val COMMAND_STRING = "worldeditaliases|wealiases"
        private const val ALIASES_SUGGESTIONS = "aliases-suggestions"
        private const val BLOCK_SUGGESTIONS = "block-suggestions"
    }

    @Suggestions(ALIASES_SUGGESTIONS)
    fun aliasesSuggestions(source: CommandContext<Source>, input: String): List<String> =
        aliases.keys.filter { it.startsWith(input) }

    @Suggestions(BLOCK_SUGGESTIONS)
    fun blockSuggestions(source: CommandContext<Source>, input: String): List<String> =
        SuggestionHelper.getNamespacedRegistrySuggestions(BlockType.REGISTRY, input).toList()

    @Command(COMMAND_STRING)
    @CommandDescription("Base command for WorldEditAliases.")
    fun help(context: CommandContext<Source>) {
        context.sender().source().sendTranslatedList("command.aliases-help")
    }

    @Command("$COMMAND_STRING add <block> <alias>")
    @CommandDescription("Add an alias for a block.")
    @Permission("worldeditaliases.command.add")
    fun add(
        context: CommandContext<Source>,
        @Argument("block", suggestions = BLOCK_SUGGESTIONS) block: String,
        @Argument("alias") alias: String
    ) {
        if (aliases.containsKey(alias)) {
            context.sender().source().sendTranslated("command.aliases-exists", "alias" to Component.text(alias))
            return
        }
        aliases[alias] = block
        Bukkit.getScheduler().runTaskAsynchronously(instance, Runnable { instance.saveAliasesToConfig() })
        context.sender().source().sendTranslated("command.aliases-set",
            "alias" to Component.text(alias),
            "block" to Component.text(block)
        )
    }

    @Command("$COMMAND_STRING remove <alias>")
    @CommandDescription("Remove an alias.")
    @Permission("worldeditaliases.command.remove")
    fun remove(
        context: CommandContext<Source>,
        @Argument("alias", suggestions = ALIASES_SUGGESTIONS)
        alias: String
    ) {
        if (!aliases.containsKey(alias)) {
            context.sender().source().sendTranslated("command.aliases-not-exists", "alias" to Component.text(alias))
            return
        }
        aliases.remove(alias)
        instance.saveAliasesToConfig()
        context.sender().source().sendTranslated("command.aliases-removed", "alias" to Component.text(alias))
    }

    @Command("$COMMAND_STRING list")
    @CommandDescription("List all aliases.")
    @Permission("worldeditaliases.command.list")
    fun list(context: CommandContext<Source>) {
        aliases.forEach { (alias, block) ->
            context.sender().source().sendTranslated("command.aliases-list-entry",
                "alias" to Component.text(alias),
                "block" to Component.text(block)
            )
        }
    }

    @Command("$COMMAND_STRING reload")
    @CommandDescription("Reload the aliases.")
    @Permission("worldeditaliases.command.reload")
    fun reload(context: CommandContext<Source>) {
        Bukkit.getScheduler().runTaskAsynchronously(instance, Runnable { instance.loadConfigurations() })
        context.sender().source().sendTranslated("command.aliases-reloaded")
    }

}
