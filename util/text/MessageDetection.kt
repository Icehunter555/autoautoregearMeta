package dev.wizard.meta.util.text

import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.entity.EntityPlayerSP

object MessageDetection {
    enum class Command : PrefixDetector {
        TROLL_HACK {
            override val prefixes: Array<out CharSequence>
                get() = arrayOf(CommandManager.prefix)
        },
        BARITONE {
            override val prefixes: Array<out CharSequence>
                get() = arrayOf(BaritoneUtils.prefix, CommandManager.prefix + 'b', ".b")
        },
        ANY {
            override val prefixes: Array<out CharSequence>
                get() = arrayOf("/", ",", ".", "-", ";", "?", "*", "^", "&", "#", "$", "!", "@", CommandManager.prefix)
        }
    }

    enum class Direct(vararg val regexes: Regex) : RegexDetector, PlayerDetector {
        SENT(Regex("^To (\\w+?): ", RegexOption.IGNORE_CASE)),
        RECEIVE(
            Regex("^(\\w+?) whispers( to you)?: "),
            Regex("^\[?(\\w+?)( )?->( )?\\\w+?]?( )?:? "),
            Regex("^From (\\w+?): ", RegexOption.IGNORE_CASE),
            Regex("^. (\\w+?) » \\[\\w+? » ")
        ),
        ANY(*SENT.regexes, *RECEIVE.regexes);

        override fun getRegexes(): Array<out Regex> = regexes

        override fun playerName(input: CharSequence): String? {
            val regex = matchedRegex(input)
            return if (regex != null) {
                val name = regex.replace(input, "$1")
                if (name.isNotBlank()) name else null
            } else null
        }
    }

    enum class Message : Detector, PlayerDetector, RemovableDetector {
        SELF {
            override fun detect(input: CharSequence): Boolean {
                val name = Wrapper.player?.name ?: return false
                return input.startsWith("<") + name + ">")
            }

            override fun playerName(input: CharSequence): String? {
                return if (detectNot(input)) null else Wrapper.player?.name
            }
        },
        OTHER {
            private val regex = Regex("^<(\\w+)>\")

            override fun detect(input: CharSequence): Boolean {
                return playerName(input) != null
            }

            override fun playerName(input: CharSequence): String? {
                val name = Wrapper.player?.name ?: return null
                val match = regex.find(input)?.groupValues?.getOrNull(1)
                return if (!match.isNullOrBlank() && match != name) match else null
            }
        },
        ANY {
            private val regex = Regex("^<(\\w+)>\")

            override fun detect(input: CharSequence): Boolean {
                return regex.containsMatchIn(input)
            }

            override fun playerName(input: CharSequence): String? {
                val match = regex.find(input)?.groupValues?.getOrNull(1)
                return if (!match.isNullOrBlank()) match else null
            }
        };

        override fun removedOrNull(input: CharSequence): CharSequence? {
            val name = playerName(input) ?: return null
            return input.toString().removePrefix("<") + name + ">")
        }
    }

    enum class Other(vararg val regexes: Regex) : RegexDetector {
        BARITONE(Regex("^\[B(aritone)?]")),
        TPA_REQUEST(Regex("^\\w+? (has requested|wants) to teleport to you\."));

        override fun getRegexes(): Array<out Regex> = regexes
    }

    enum class Server(vararg val regexes: Regex) : RegexDetector {
        QUEUE(Regex("^Position in queue: ")),
        QUEUE_IMPORTANT(Regex("^Position in queue: [1-5]$")),
        RESTART(Regex("^\[SERVER] Server restarting in ")),
        ANY(*QUEUE.regexes, *RESTART.regexes);

        override fun getRegexes(): Array<out Regex> = regexes
    }
}
