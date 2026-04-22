package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.ClipboardUtils
import dev.wizard.meta.util.text.NoSpamMessage

object SendCoords : ClientCommand("sendcoords", arrayOf("sc"), "Send coordinates with various options") {

    private val taunts = listOf(
        "Come get me nigga", "You wont pull up tho ur too pussy", "Im ur owner nigga, pull up",
        "cmere and die pussy", "Come test that ego, I'll deflate it quick.", "come fight me bitch",
        "i own this shit", "but you wont pull up cause ur too scared LOL", "but ur too trash to fight me LOL",
        "pull up if not scared", "im ur dad nigga pull up", "but ur just gonna dodge LELELEL",
        "ur gonna be quickdropped lol", "these fags too ez, try and do better", "pull up or ez dodge",
        "ur too scared to be seen here tho", "cmere to die"
    )

    private fun getDim(): String {
        return when (mc.player.dimension) {
            0 -> "Overworld"
            -1 -> "Nether"
            1 -> "End"
            else -> "Unknown Dimension"
        }
    }

    private fun pos(): String {
        val posx = mc.player.posX.toInt()
        val posy = mc.player.posY.toInt()
        val posz = mc.player.posZ.toInt()
        return "$posx, $posy, $posz"
    }

    private fun coordsMessage(): String {
        return "My coordinates are ${pos()} in the ${getDim()}"
    }

    private fun sendMessage(str: String) {
        mc.player.sendChatMessage(str)
    }

    private fun sendWhisper(player: String, str: String) {
        mc.player.sendChatMessage("/w $player $str")
    }

    fun backupRequest(): String {
        return "need backup at ${pos()} in the ${getDim()}!"
    }

    private fun copyToClipboard() {
        try {
            val p = pos()
            ClipboardUtils.copyToClipboard(p)
            NoSpamMessage.sendMessage("Coordinates copied to clipboard: $p")
        } catch (e: Exception) {
            NoSpamMessage.sendError("Failed to copy coordinates to clipboard: ${e.message}")
        }
    }

    init {
        executeSafe("Send coords in chat") {
            sendMessage(coordsMessage())
        }

        literal("p", "player", "whisper") {
            player("player") { playerArg ->
                executeSafe("Send coords to a player") {
                    sendWhisper(getValue(playerArg).name, coordsMessage())
                }

                literal("a") {
                    greedy("message") { msgArg ->
                        executeSafe("Send coords to a player with a message") {
                            sendWhisper(getValue(playerArg).name, "${coordsMessage()}  ${getValue(msgArg)}")
                        }
                    }
                }

                literal("i", "insult", "taunt") {
                    executeSafe("Send coords and insult to a player") {
                        val taunt = taunts.random()
                        sendWhisper(getValue(playerArg).name, "${coordsMessage()} $taunt")
                    }
                }

                literal("b", "backup") {
                    executeSafe("Send backup request to a player") {
                        sendWhisper(getValue(playerArg).name, backupRequest())
                    }
                }
            }
        }

        literal("a") {
            greedy("message") { msgArg ->
                executeSafe("Sends coords in chat with an appended message") {
                    sendMessage("${coordsMessage()} ${getValue(msgArg)}")
                }
            }
        }

        literal("i", "insult", "taunt") {
            executeSafe("Send coords in chat with an insult") {
                val taunt = taunts.random()
                sendMessage("${coordsMessage()} $taunt")
            }

            player("player") { playerArg ->
                executeSafe {
                    val taunt = taunts.random()
                    sendWhisper(getValue(playerArg).name, "${coordsMessage()} $taunt")
                }
            }
        }

        literal("l", "clipboard") {
            executeSafe {
                copyToClipboard()
            }
        }

        literal("b", "backup") {
            executeSafe("Request backup in chat") {
                sendMessage(backupRequest())
            }

            player("player") { playerArg ->
                executeSafe {
                    sendWhisper(getValue(playerArg).name, backupRequest())
                }
            }
        }

        literal("ip", "pi") {
            player("player") { playerArg ->
                executeSafe {
                    val taunt = taunts.random()
                    sendWhisper(getValue(playerArg).name, "${coordsMessage()} $taunt")
                }
            }
        }

        literal("bp", "pb") {
            player("player") { playerArg ->
                executeSafe {
                    sendWhisper(getValue(playerArg).name, backupRequest())
                }
            }
        }

        literal("pa", "ap") {
            player("player") { playerArg ->
                greedy("message") { msgArg ->
                    executeSafe {
                        sendWhisper(getValue(playerArg).name, "${coordsMessage()}  ${getValue(msgArg)}")
                    }
                }
            }
        }

        player("player") { playerArg ->
            executeSafe("Send coords to player") {
                sendWhisper(getValue(playerArg).name, coordsMessage())
            }
        }
    }
}
