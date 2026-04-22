package dev.wizard.meta.command.commands

import dev.wizard.meta.command.BlockPosArg
import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.manager.managers.WaypointManager
import dev.wizard.meta.module.modules.movement.AutoWalk
import dev.wizard.meta.util.InfoCalculator
import dev.wizard.meta.util.math.CoordinateConverter
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue
import net.minecraft.util.math.BlockPos

object WaypointCommand : ClientCommand("waypoint", arrayOf("wp"), "Manages waypoint.") {

    private val stashRegex = Regex("\\(\\d+ chests, \\d+ shulkers, \\d+ droppers, \\d+ dispensers, \\d+ hoppers\\)")
    private var confirmTime = 0L

    private fun add(name: String, pos: BlockPos) {
        WaypointManager.add(pos, name)
        NoSpamMessage.sendMessage(this, "Added waypoint at ${CoordinateConverter.asString(pos)} in the ${InfoCalculator.dimension()} with name '\u00a77$name\u00a7f'.")
    }

    private fun delete(id: Int) {
        if (WaypointManager.remove(id)) {
            NoSpamMessage.sendMessage(this, "Removed waypoint with ID $id")
        } else {
            NoSpamMessage.sendMessage(this, "No waypoint with ID $id")
        }
    }

    private fun goTo(id: Int) {
        val waypoint = WaypointManager.get(id)
        if (waypoint != null) {
            if (AutoWalk.isEnabled) AutoWalk.disable()
            val pos = waypoint.currentPos()
            MessageSendUtils.sendBaritoneCommand("goto", pos.x.toString(), pos.y.toString(), pos.z.toString())
        } else {
            NoSpamMessage.sendMessage(this, "Couldn't find a waypoint with the ID $id")
        }
    }

    private fun goTo(x: Int, y: Int, z: Int) {
        if (AutoWalk.isEnabled) AutoWalk.disable()
        MessageSendUtils.sendBaritoneCommand("goto", x.toString(), y.toString(), z.toString())
    }

    private fun list() {
        if (WaypointManager.waypoints.isEmpty()) {
            NoSpamMessage.sendMessage(this, "No waypoints have been saved.")
        } else {
            val stringBuilder = StringBuilder("List of waypoints:\n")
            WaypointManager.waypoints.forEach {
                stringBuilder.append("${format(it)}\n")
            }
            NoSpamMessage.sendMessage(this, stringBuilder.toString())
        }
    }

    private fun stash() {
        val filtered = WaypointManager.waypoints.filter { stashRegex.matches(it.name) }
        if (filtered.isEmpty()) {
            NoSpamMessage.sendMessage(this, "No stashes have been logged.")
        } else {
            val stringBuilder = StringBuilder("List of logged stashes:\n")
            filtered.forEach {
                stringBuilder.append("${format(it)}\n")
            }
            NoSpamMessage.sendMessage(this, stringBuilder.toString())
        }
    }

    private fun search(name: String) {
        val filtered = WaypointManager.waypoints.filter { it.name.equals(name, ignoreCase = true) }
        if (filtered.isEmpty()) {
            NoSpamMessage.sendMessage(this, "No results for \u00a77$name\u00a7f")
        } else {
            val stringBuilder = StringBuilder("Result of search for \u00a77$name\u00a7f:\n")
            filtered.forEach {
                stringBuilder.append("${format(it)}\n")
            }
            NoSpamMessage.sendMessage(this, stringBuilder.toString())
        }
    }

    private fun clear() {
        if (System.currentTimeMillis() - confirmTime > 15000L) {
            confirmTime = System.currentTimeMillis()
            NoSpamMessage.sendWarning("This will delete ALL your waypoints, run ${("$prefixName clear").formatValue()} again to confirm")
        } else {
            confirmTime = 0L
            WaypointManager.clear()
            NoSpamMessage.sendMessage(this, "Waypoints have been \u00a7ccleared")
        }
    }

    private fun format(waypoint: WaypointManager.Waypoint): String {
        return "${waypoint.id} [${waypoint.server}] ${waypoint.name} (${CoordinateConverter.bothConverted(waypoint.dimension, waypoint.pos)})"
    }

    init {
        literal("add", "new", "create", "+") {
            string("name") { nameArg ->
                blockPos("pos") { posArg ->
                    execute {
                        add(getValue(nameArg), getValue(posArg))
                    }
                }

                int("x") { xArg ->
                    int("y") { yArg ->
                        int("z") { zArg ->
                            execute {
                                add(getValue(nameArg), BlockPos(getValue(xArg), getValue(yArg), getValue(zArg)))
                            }
                        }
                    }
                }

                executeSafe {
                    add(getValue(nameArg), player.position)
                }
            }

            executeSafe {
                add("Unnamed", player.position)
            }
        }

        literal("del", "remove", "delete", "-") {
            int("id") { idArg ->
                executeAsync {
                    delete(getValue(idArg))
                }
            }
        }

        literal("goto", "path") {
            int("id") { idArg ->
                execute {
                    goTo(getValue(idArg))
                }
            }

            blockPos("pos") { posArg ->
                execute {
                    val pos = getValue(posArg)
                    goTo(pos.x, pos.y, pos.z)
                }
            }

            int("x") { xArg ->
                int("y") { yArg ->
                    int("z") { zArg ->
                        execute {
                            goTo(getValue(xArg), getValue(yArg), getValue(zArg))
                        }
                    }
                }
            }
        }

        literal("list") {
            execute {
                list()
            }
        }

        literal("stash", "stashes") {
            executeAsync {
                stash()
            }
        }

        literal("search") {
            string("name") { nameArg ->
                executeAsync {
                    search(getValue(nameArg))
                }
            }
        }

        literal("clear") {
            execute {
                clear()
            }
        }
    }
}
