package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.util.text.MessageSendUtils

object BaritoneCommand : ClientCommand("baritone", arrayOf("b")) {

    private fun exec(vararg args: String) {
        val safeArgs = CommandManager.tryParseArgument(args.joinToString(" ")) ?: return
        MessageSendUtils.sendBaritoneCommand(*safeArgs)
    }

    init {
        literal("thisway") {
            int("blocks") { blocksArg ->
                executeSafe("Walk in the current direction for X blocks.") {
                    exec("thisway", getValue(blocksArg).toString())
                    exec("path")
                }
            }
        }

        literal("goal") {
            literal("clear") {
                executeSafe("Clear the current goal.") {
                    exec("goal", "clear")
                }
            }

            int("x/y") { xArg ->
                executeSafe("Set goal to a Y level.") {
                    exec("goal", getValue(xArg).toString())
                }

                int("y/z") { yArg ->
                    executeSafe("Set goal to X Z.") {
                        exec("goal", getValue(xArg).toString(), getValue(yArg).toString())
                    }

                    int("z") { zArg ->
                        executeSafe("Set goal to X Y Z.") {
                            exec("goal", getValue(xArg).toString(), getValue(yArg).toString(), getValue(zArg).toString())
                        }
                    }
                }
            }
        }

        literal("path") {
            executeSafe("Start pathing towards your set goal.") {
                exec("path")
            }
        }

        literal("stop", "cancel") {
            executeSafe("Stop the current Baritone process.") {
                exec("stop")
            }
        }

        literal("mine") {
            block("block") { blockArg ->
                executeSafe("Mine a block.") {
                    exec("mine", getValue(blockArg).registryName?.path ?: return@executeSafe)
                }
            }

            greedy("blocks") { blocksArg ->
                executeSafe("Mine any amount of blocks.") {
                    exec("mine", getValue(blocksArg))
                }
            }
        }

        literal("follow") {
            player("player") { playerArg ->
                executeSafe("Follow a player.") {
                    exec("follow", "players", getValue(playerArg).name)
                }
            }
        }

        literal("goto") {
            blockPos("coordinates") { coordinatesArg ->
                executeSafe("Go to a set of coordinates.") {
                    val coord = getValue(coordinatesArg)
                    exec("goto", coord.x.toString(), coord.y.toString(), coord.z.toString())
                }
            }

            baritoneBlock("cached block") { blockArg ->
                executeSafe("Go to a Baritone cached block.") {
                    exec("goto", getValue(blockArg).registryName?.path ?: return@executeSafe)
                }
            }

            greedy("x y z") { coordinatesArg ->
                executeSafe("Go to a set of coords. Y and Z optional.") {
                    exec("goto", getValue(coordinatesArg))
                }
            }
        }

        literal("click") {
            executeSafe("Open the click and drag menu.") {
                exec("click")
            }
        }

        literal("build") {
            schematic("schematic") { schematicArg ->
                executeSafe("Build something from inside the schematics folder.") {
                    exec("build", getValue(schematicArg).name)
                }
            }
        }

        literal("schematica") {
            executeSafe("Build the currently opened schematic in Schematica.") {
                exec("schematica")
            }

            greedy("args") { greedyArg ->
                executeSafe("Build the currently opened schematic in Schematica.") {
                    exec("schematica", getValue(greedyArg))
                }
            }
        }

        literal("farm") {
            executeSafe("Automatically farm any found crops.") {
                exec("farm")
            }
        }

        literal("explore") {
            executeSafe("Explore away from you.") {
                exec("explore")
            }

            int("x") { xArg ->
                int("z") { zArg ->
                    executeSafe("Explore away from X Z.") {
                        exec("explore", getValue(xArg).toString(), getValue(zArg).toString())
                    }
                }
            }
        }

        literal("invert") {
            executeSafe("Go in the opposite direction of your goal.") {
                exec("invert")
            }
        }

        literal("version") {
            executeSafe {
                exec("version")
                MessageSendUtils.sendBaritoneMessage("Running on Meta 0.3B-10mq29")
            }
        }

        greedy("arguments") { argsArg ->
            executeSafe {
                exec(getValue(argsArg))
            }
        }
    }
}
