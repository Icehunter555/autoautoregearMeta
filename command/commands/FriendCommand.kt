package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.util.text.NoSpamMessage

object FriendCommand : ClientCommand("friend", arrayOf("f"), "Add someone as your friend!") {

    private var confirmTime = 0L

    private fun isFriend(name: String) {
        val string = if (FriendManager.isFriend(name)) "Yes, $name is your friend." else "No, $name isn't a friend of yours."
        NoSpamMessage.sendMessage(string)
    }

    private fun listFriends() {
        if (FriendManager.empty) {
            NoSpamMessage.sendMessage("You currently don't have any friends added. run \u00a77${prefix}friend add <name>\u00a7r to add one.")
        } else {
            val f = FriendManager.friends.values.joinToString("\n    ", prefix = "\n    ") { it.name }
            NoSpamMessage.sendMessage("Your friends: $f")
        }
    }

    init {
        literal("add", "new", "+") {
            player("player") { playerArg ->
                execute {
                    val name = getValue(playerArg).name
                    if (FriendManager.isFriend(name)) {
                        NoSpamMessage.sendMessage("That player is already your friend.")
                    } else if (FriendManager.addFriend(name)) {
                        NoSpamMessage.sendMessage("\u00a77$name\u00a7r has been friended.")
                    } else {
                        NoSpamMessage.sendMessage("Failed to find UUID of $name")
                    }
                }
            }
        }

        literal("del", "remove", "-") {
            player("player") { playerArg ->
                execute {
                    val name = getValue(playerArg).name
                    if (FriendManager.removeFriend(name)) {
                        NoSpamMessage.sendMessage("\u00a77$name\u00a7r has been unfriended.")
                    } else {
                        NoSpamMessage.sendMessage("That player isn't your friend.")
                    }
                }
            }
        }

        literal("toggle") {
            execute {
                FriendManager.enabled = !FriendManager.enabled
                if (FriendManager.enabled) {
                    NoSpamMessage.sendMessage("Friends have been \u00a7aenabled")
                } else {
                    NoSpamMessage.sendMessage("Friends have been \u00a7cdisabled")
                }
            }
        }

        literal("clear") {
            execute {
                if (System.currentTimeMillis() - confirmTime > 15000L) {
                    confirmTime = System.currentTimeMillis()
                    NoSpamMessage.sendMessage("This will delete ALL your friends, run \u00a77${prefix}friend clear\u00a7f again to confirm")
                } else {
                    confirmTime = 0L
                    FriendManager.clearFriend()
                    NoSpamMessage.sendMessage("Friends have been \u00a7ccleared")
                }
            }
        }

        literal("is") {
            player("player") { playerArg ->
                execute {
                    isFriend(getValue(playerArg).name)
                }
            }
        }

        literal("list") {
            execute {
                listFriends()
            }
        }

        execute {
            listFriends()
        }
    }
}
