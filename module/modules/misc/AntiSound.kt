package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.SoundPlayedEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.SoundCategory

object AntiSound : Module(
    name = "AntiSound",
    category = Category.MISC,
    description = "Stop some sounds"
) {
    private val noSoundLag by setting("No Sound Lag", true)
    private val noEat by setting("No Eating", false)
    private val noExplosion by setting("No Explosions", false)
    private val noMineCart by setting("No MineCart", false)
    private val noPortal by setting("No Portal", false)
    private val noElytra by setting("No Elytra", false)
    private val noWither by setting("No Wither", false)
    private val noCaveSounds by setting("No Cave Sounds", false)
    private val noNoteBlock by setting("No NoteBlock", false)

    init {
        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketSoundEffect) return@listener
            val packet = it.packet as SPacketSoundEffect
            
            if (packet.category == SoundCategory.PLAYERS) {
                if (noSoundLag && (packet.sound == SoundEvents.ITEM_ARMOR_EQUIP_GENERIC || packet.sound == SoundEvents.ITEM_SHIELD_BREAK)) {
                    it.cancel()
                }
                if (noEat && packet.sound == SoundEvents.ENTITY_PLAYER_BURP) {
                    it.cancel()
                }
            }
            
            if (noExplosion && packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                it.cancel()
            }
            
            if (noMineCart && (packet.sound == SoundEvents.ENTITY_MINECART_RIDING || packet.sound == SoundEvents.ENTITY_MINECART_INSIDE)) {
                it.cancel()
            }
        }

        listener<SoundPlayedEvent> {
            val name = it.name.lowercase()
            if (noMineCart && name.contains("entity.minecart")) it.resultSound = null
            if (noPortal && name.contains("block.portal")) it.resultSound = null
            if (noElytra && name.contains("item.elytra")) it.resultSound = null
            if (noWither && name.contains("entity.wither")) it.resultSound = null
            if (noCaveSounds && name.contains("ambient.cave")) it.resultSound = null
            if (noNoteBlock && name.contains("block.note")) it.resultSound = null
        }
    }
}
