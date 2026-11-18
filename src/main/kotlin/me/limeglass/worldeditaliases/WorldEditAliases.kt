package me.limeglass.worldeditaliases

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extension.input.InputParseException
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.internal.registry.InputParser
import com.sk89q.worldedit.world.block.BaseBlock
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper
import org.incendo.cloud.paper.util.sender.Source
import java.io.File

class WorldEditAliases : JavaPlugin() {

    companion object {
        @JvmStatic
        var TESTING: Boolean = System.getProperty("worldeditaliases.testing")?.toBoolean() ?: false
    }

    internal val aliases = mutableMapOf<String, String>()
    private lateinit var aliasesConfig: YamlConfiguration
    private lateinit var aliasesFile: File
    private var integrationTest: IntegrationTest? = null

    override fun onEnable() {
        if (TESTING) {
            integrationTest = IntegrationTest(this)
        }
        CommandSenderExt.miniMessage = MiniMessage.miniMessage()
        CommandSenderExt.adventure = BukkitAudiences.create(this)
        CommandSenderExt.config = config
        val commandManager: PaperCommandManager<Source> = PaperCommandManager
            .builder(PaperSimpleSenderMapper.simpleSenderMapper())
            .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
            .buildOnEnable(this)

        AnnotationParser(commandManager, Source::class.java).apply {
            installCoroutineSupport()
            parse(Commands(aliases, this@WorldEditAliases))
        }

        loadConfigurations()

        WorldEdit.getInstance().blockFactory.register(object : InputParser<BaseBlock>(WorldEdit.getInstance()) {
            @Throws(InputParseException::class)
            override fun parseFromInput(input: String, context: ParserContext): BaseBlock? {
                return aliases[input]?.let { WorldEdit.getInstance().blockFactory.parseFromInput(it, context) }
            }
        })

        if (TESTING) {
            integrationTest?.start()
        }
    }

    fun loadConfigurations() {
        saveResource("config.yml", false)
        saveDefaultConfig()

        aliasesFile = File(dataFolder, "aliases.yml")
        if (!aliasesFile.exists()) {
            aliasesFile.createNewFile()
        }
        aliasesConfig = YamlConfiguration.loadConfiguration(aliasesFile)
        aliasesConfig.getList("aliases")?.forEach {
            if (it is LinkedHashMap<*, *>) {
                val alias = it["alias"]
                val block = it["block"]
                if (alias is String && block is String) {
                    aliases[alias] = block
                }
            }
        }
    }

    override fun onDisable() {
        if (this::aliasesConfig.isInitialized) {
            saveAliasesToConfig()
        }
        integrationTest?.onPluginDisabled()
    }

    fun saveAliasesToConfig() {
        aliasesConfig.set("aliases", aliases.map { mapOf("alias" to it.key, "block" to it.value) })
        aliasesConfig.save(aliasesFile)
    }
}
