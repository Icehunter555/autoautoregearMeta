package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketChat
import kotlin.random.Random

object UnicodeLag : Module(
    name = "UnicodeLag",
    category = Category.MISC,
    description = "Allows you to prevent unicode lag, and to unicode lag others",
    modulePriority = Int.MAX_VALUE
) {
    private val antiUnicodeLag by setting("Anti-Unicode", true)
    private val antiUnicodeThreshold by setting("Anti-Unicode Threshold", 20, 5..100, 10) { antiUnicodeLag }
    private val autoLoadUnicodeLag by setting("Auto Load Unicode", false)
    private val randomUnicodeMessage by setting("Random Unicode Message", false)
    private val unicodeLoadDelay by setting("Unicode Load Delay", 1, 1..10, 1) { autoLoadUnicodeLag }
    private val popLag by setting("PopLag", true)
    private val popLagDelay by setting("Pop Lag Delay", 1, 0..10, 1) { popLag }
    private val targetLag by setting("Target Lag", false)
    private val targetLagDelay by setting("Target lag Delay", 1, 1..10, 1) { targetLag }

    private const val LAG_TEXT = "\u0101\u0201\u0301\u0401\u0601\u0701\u0801\u0901\u0a01\u0b01\u0e01\u0f01\u1001\u1101\u1201\u1301\u1401\u1501\u1601\u1701\u1801\u1901\u1a01\u1b01\u1c01\u1d01\u1e01\u1f01\u2101\u2201\u2301\u2401\u2501\u2701\u2801\u2901\u2a01\u2b01\u2c01\u2d01\u2e01\u2f01\u3001\u3101\u3201\u3301\u3401\u3501\u3601\u3701\u3801\u3901\u3a01\u3b01\u3c01\u3d01\u3e01\u3f01\u4001\u4101\u4201\u4301\u4401\u4501\u4601\u4701\u4801\u4901\u4a01\u4b01\u4c01\u4d01\u4e01\u4f01\u5001\u5101\u5201\u5301\u5401\u5501\u5601\u5701\u5801\u5901\u5a01\u5b01\u5c01\u5d01\u5e01\u5f01\u6001\u6101\u6201\u6301\u6401\u6501\u6601\u6701\u6801\u6901\u6a01\u6b01\u6c01\u6d01\u6e01\u6f01\u7001\u7101\u7201\u7301\u7401\u7501\u7601\u7701\u7801\u7901\u7a01\u7b01\u7c01\u7d01\u7e01\u7f01\u8001\u8101\u8201\u8301\u8401\u8501\u8601\u8701\u8801\u8901\u8a01\u8b01\u8c01\u8d01\u8e01\u8f01\u9001\u9101\u9201\u9301\u9401\u9501\u9601\u9701\u9801\u9901\u9a01\u9b01\u9c01\u9d01\u9e01\u9f01\ua001\ua101\ua201\ua301\ua401\ua501\ua601\ua701\ua801\ua901\uaa01\uab01\uac01\uad01\uae01\uaf01\ub001\ub101\ub201\ub301\ub401\ub501\ub601\ub701\ub801\ub901\uba01\ubb01\ubc01\ubd01"
    private const val LAG_CHARS = "\u77e9\u7d0c\u9c68\u5dfb\u917b\u7cb3\u557a\u6cdd\u9de5\u71f1\u9dda\u9264\u606e\u5ccc\u91af\u7904\u8618\u5ebf\u7a40\u5898\u79b1\u7682\u87f2\u8133\u6dba\u74f6\u854f\u86ac\u5f13\u77b5\u73f1\u7e8c\u9cfe\u55f4\u525b\u5fe0\u4fae\u9af9\u5de1\u8de0\u8c44\u7267\u7349\u6c80\u9796\u691e\u7103\u9021\u5504\u8a12\u8229\u85e1\u9624\u6eed\u79cd\u9695\u8112\u632a\u7ce2\u5d8a\u5cfa\u4e7e\u6d38\u710a\u6307\u7ffa\u834c\u6113\u5bae\u70e0\u9e5a\u9aed\u7a5f\u7c88\u9a82\u5ba7\u89e9\u6f6b\u87ed\u6e3e\u9311\u831f\u65c1\u99c8\u73f8\u6076\u7e2e\u5cc6\u995e\u535e\u98e5\u6002\u7411\u633c\u72d8\u9730\u7d78\u5c70\u738e\u6499\u690f\u66ba\u680f\u72e6\u63f9\u9630\u557e\u54fe\u915b\u5a06\u5efb\u8944\u5a75\u5283\u7562\u6392\u6023\u606d\u7dd9\u56d4\u647f\u8169\u85e4\u5758\u6938\u9173\u7577\u615f\u7e02\u99b5\u8d81\u7ae8\u9c31\u5819\u9963\u6133\u8074\u84bb\u68b1\u7c6c\u91fd\u9da3\u8adb\u80df\u60e6\u94f5\u8b80\u714d\u7759\u7353\u6110\u6442\u87f1\u4f99\u93c7\u50d9\u684f\u79b2\u6627\u9a44\u7264\u998d\u6a67\u90e5\u548e\u71cd\u98d9\u8e96\u56bd\u66f2\u764b\u6552\u8129\u7aa4\u5f0c\u5343\u5480\u4ea4\u8325\u632e\u7feb\u80ca\u6a18\u5700\u7c52\u7d01\u7bad\u5889\u63e6\u73f0\u6a77\u7429\u75fd\u7bdd\u876e\u9f24\u66f8\u7ba3\u9e2c\u9892\u7867\u7fed\u5b30\u4e47\u93fa\u600a\u5637\u5ecd\u5fca\u5347\u8b01\u5cb5\u8550\u51b0\u5259\u5e1f\u9047\u9bb6\u6941\u592e\u8ec3\u6274\u56a8\u7487\u63c5\u9627\u70cb\u9e17\u8e6f\u97bb\u885a\u921a\u575b\u54b5\u75de\u67fb\u7510\u7011\u9904\u5ee9\u7bb7\u6dc8\u8129\u5355\u7e64\u56b8\u55a1\u65fd\u8a40\u8ef8\u6dfb\u612c\u552f\u7969\u602b\u5a5b\u9c18\u677e\u9a05\u5d1d\u6bd1\u88b4\u82c7\u9f27\u9d40\u5849\u5bee\u7fa5\u7789\u807a\u9ef0\u7b98\u6639\u5a3e\u59ac\u5587\u8e18\u5b1f\u72f7\u8232\u9e8e\u6d0b\u8929\u6413\u55a5\u565b\u6036\u981f\u7ef9\u86c4\u9271\u960d\u54fa\u85ea\u6a51\u522b\u7ad7\u9bad\u87ca\u881f\u5b24\u8ef5\u7b37\u9151\u89b6\u57a5\u72c8\u8c38\u4fac\u550e\u84f3\u9c21\u6f17\u4fef\u6f67\u69fb\u83a0\u6141\u90b9\u9ce1\u659b\u9cc4\u59ec\u6db0\u8846\u6ae6"
    private val playerNameRegex = Regex("<(.+?)>")
    
    private val targetLagMap = LinkedHashMap<EntityPlayer, Long>()
    private val laggedPlayerMap = LinkedHashMap<EntityPlayer, Long>()
    private var lastAutoLoad = 0L

    init {
        onEnable {
            reset()
        }

        onDisable {
            reset()
        }

        listener<WorldEvent.Unload> {
            reset()
        }

        listener<TotemPopEvent.Pop> {
            if (!popLag || EntityUtils.isFriend(it.entity) || EntityUtils.isSelf(it.entity)) return@listener

            if (!laggedPlayerMap.containsKey(it.entity)) {
                laggedPlayerMap[it.entity] = System.currentTimeMillis()
                player.sendChatMessage("/msg ${it.name} ${getLagText()}")
            } else if (System.currentTimeMillis() - laggedPlayerMap[it.entity]!! > popLagDelay * 60000) {
                laggedPlayerMap[it.entity] = System.currentTimeMillis()
                player.sendChatMessage("/msg ${it.name} ${getLagText()}")
            }
        }

        listener<TickEvent.Post> {
            val target = CombatManager.target
            if (targetLag && target is EntityPlayer) {
                if (!targetLagMap.containsKey(target)) {
                    targetLagMap[target] = System.currentTimeMillis()
                    player.sendChatMessage("/msg ${target.name} ${getLagText()}")
                } else if (System.currentTimeMillis() - targetLagMap[target]!! > targetLagDelay * 60000) {
                    targetLagMap[target] = System.currentTimeMillis()
                    player.sendChatMessage("/msg ${target.name} ${getLagText()}")
                }
            }

            if (autoLoadUnicodeLag && System.currentTimeMillis() - lastAutoLoad > unicodeLoadDelay * 60000) {
                lastAutoLoad = System.currentTimeMillis()
                NoSpamMessage.sendMessage("$chatName Auto loading unicode lag:")
                NoSpamMessage.sendMessage(getLagText())
                sendLog("Loaded unicode lag")
            }
        }

        listener<PacketEvent.Receive>(-2933) {
            if (antiUnicodeLag && it.packet is SPacketChat) {
                val message = it.packet.chatComponent.unformattedText
                val unicodeCount = message.count { it > '\u007F' }

                if (unicodeCount >= antiUnicodeThreshold) {
                    it.cancel()
                    val sender = getSender(it.packet as SPacketChat)
                    if (sender != null && !message.contains(mc.player.name)) {
                        NoSpamMessage.sendMessage("Unicode Message from $sender blocked!")
                        sendLog("Unicode Message from $sender blocked")
                    }
                }
            }
        }
    }

    private fun reset() {
        laggedPlayerMap.clear()
        lastAutoLoad = 0L
        targetLagMap.clear()
    }

    private fun getSender(packet: SPacketChat): String? {
        val message = packet.chatComponent.unformattedText
        val onlinePlayers = mc.connection?.playerInfoMap?.map { it.gameProfile.name } ?: emptyList()

        if (message.contains(" whispers: ")) {
            return message.substringBefore(" whispers: ").trim()
        }

        if (!playerNameRegex.containsMatchIn(message)) return null

        val sender = onlinePlayers.firstOrNull { 
            val possibleName = message.substringAfter("<").substringBefore(">").trim()
            it == possibleName
        }

        return sender
    }

    private fun getLagText(): String {
        return if (!randomUnicodeMessage) {
            LAG_TEXT
        } else {
            val sb = StringBuilder()
            var lastChar = ' '
            var repeatCount = 0
            for (i in 0 until 194) {
                var c = LAG_CHARS[Random.nextInt(LAG_CHARS.length)]
                while (c == lastChar && repeatCount >= 5) {
                    c = LAG_CHARS[Random.nextInt(LAG_CHARS.length)]
                }
                if (c == lastChar) {
                    repeatCount++
                } else {
                    repeatCount = 1
                    lastChar = c
                }
                sb.append(c)
            }
            sb.toString()
        }
    }
}