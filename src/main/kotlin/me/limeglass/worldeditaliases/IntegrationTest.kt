package me.limeglass.worldeditaliases

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.world.block.BlockType
import org.bukkit.Bukkit
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

class IntegrationTest(private val plugin: WorldEditAliases) {

    private companion object {
        private const val AWAIT_TIMEOUT_SECONDS = 15L
        private const val REPORT_FILE = "test-results.txt"
    }

    private val failures = CopyOnWriteArrayList<String>()
    private val commandErrors = CopyOnWriteArrayList<String>()
    private val reportFile = File(plugin.dataFolder, REPORT_FILE)

    @Volatile
    private var completed = false

    private val logHandler = object : Handler() {
        override fun publish(record: LogRecord) {
            if (record.level.intValue() >= Level.SEVERE.intValue()) {
                commandErrors += buildString {
                    append(record.message)
                    record.thrown?.let {
                        append('\n')
                        append(it.stackTraceToString())
                    }
                }
            }
        }

        override fun flush() {}
        override fun close() {}
    }

    fun start() {
        plugin.logger.addHandler(logHandler)
        plugin.logger.info("[TEST] Starting integration tests")
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { runTests() })
    }

    fun onPluginDisabled() {
        if (completed) return
        completed = true
        runCatching {
            reportFile.parentFile?.mkdirs()
            reportFile.writeText("FAIL\n- Plugin was disabled before the integration tests completed\n")
        }
        plugin.logger.severe("[TEST] Plugin disabled before the integration tests completed")
        System.exit(1)
    }

    private fun runTests() {
        try {
            resetAliases()

            dispatch("wealiases")

            dispatch("wealiases add gold_block g")
            await("alias 'g' was not added") { plugin.aliases["g"] == "gold_block" }
            assertNoCommandErrors("add")

            dispatch("wealiases add diamond_block d")
            await("alias 'd' was not added") { plugin.aliases["d"] == "diamond_block" }
            assertNoCommandErrors("add (second)")

            dispatch("wealiases list")
            assertNoCommandErrors("list")

            val parsed = parseAlias("g")
            val expected = BlockType.REGISTRY.get("minecraft:gold_block")
            if (parsed?.blockType != expected) {
                fail("WorldEdit parsed alias 'g' as '${parsed?.blockType}', expected '$expected'")
            }

            dispatch("wealiases remove g")
            await("alias 'g' was not removed") { "g" !in plugin.aliases }
            assertNoCommandErrors("remove")

            dispatch("wealiases add emerald_block e")
            await("alias 'e' was not added") { plugin.aliases["e"] == "emerald_block" }
            await("alias 'e' was not persisted") {
                val file = File(plugin.dataFolder, "aliases.yml")
                file.isFile && file.readText().contains("emerald_block")
            }
            Bukkit.getScheduler().callSyncMethod(plugin, Callable { plugin.aliases.clear() })
                .get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            dispatch("wealiases reload")
            await("alias 'e' was not loaded by reload") { plugin.aliases["e"] == "emerald_block" }
            assertNoCommandErrors("reload")

            dispatch("wealiases remove e")
            await("alias 'e' was not removed") { "e" !in plugin.aliases }
            assertNoCommandErrors("cleanup")
        } catch (t: Throwable) {
            fail(t.message ?: t.toString())
        } finally {
            finish()
        }
    }

    private fun resetAliases() {
        Bukkit.getScheduler().callSyncMethod(plugin, Callable {
            plugin.aliases.clear()
            plugin.saveAliasesToConfig()
        }).get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun dispatch(command: String) {
        val known = Bukkit.getScheduler().callSyncMethod(plugin, Callable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        }).get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!known) {
            fail("Command '/$command' is not registered")
        }
    }

    private fun parseAlias(alias: String) = try {
        val context = ParserContext().apply {
            actor = BukkitAdapter.adapt(Bukkit.getConsoleSender())
            world = BukkitAdapter.adapt(Bukkit.getWorlds().first())
        }
        WorldEdit.getInstance().blockFactory.parseFromInput(alias, context)
    } catch (t: Throwable) {
        fail("Parsing alias '$alias' threw ${t.message}")
        null
    }

    private fun await(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(AWAIT_TIMEOUT_SECONDS)
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        fail(description)
    }

    private fun assertNoCommandErrors(stage: String) {
        Thread.sleep(250)
        if (commandErrors.isNotEmpty()) {
            val errors = commandErrors.joinToString("\n")
            commandErrors.clear()
            fail("Command handler error during $stage:\n$errors")
        }
    }

    private fun fail(message: String) {
        plugin.logger.severe("[TEST] $message")
        failures += message
    }

    private fun finish() {
        Thread.sleep(500)
        plugin.logger.removeHandler(logHandler)

        val passed = failures.isEmpty()
        val report = buildString {
            appendLine(if (passed) "PASS" else "FAIL")
            failures.forEach { appendLine("- $it") }
            appendLine("Aliases: ${plugin.aliases.toMap()}")
        }

        try {
            reportFile.writeText(report)
        } catch (t: Throwable) {
            plugin.logger.log(Level.SEVERE, "[TEST] Failed to write the integration test report", t)
        } finally {
            completed = true
            plugin.logger.info("[TEST] Finished integration tests:\n$report")
            Bukkit.getScheduler().runTask(plugin, Runnable { Bukkit.shutdown() })
        }
    }
}
