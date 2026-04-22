package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.combat.CrystalSpawnEvent
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.misc.Suicide
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.combat.CrystalDamage
import dev.wizard.meta.util.combat.DamageReduction
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.TaskKt
import dev.wizard.meta.util.inventory.ItemKt
import dev.wizard.meta.util.inventory.operation.moveTo
import dev.wizard.meta.util.inventory.operation.swapToItemOrMove
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.HotbarSlot
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.DefaultScope
import dev.wizard.meta.util.threads.ThreadSafetyKt
import dev.wizard.meta.util.accessor.getPotion
import kotlinx.coroutines.launch
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.entity.projectile.EntityTippedArrow
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.EnumAction
import net.minecraft.item.ItemPickaxe
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.potion.PotionUtils
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

object AutoTotem : Module(
    "AutoTotem",
    category = Category.COMBAT,
    description = "Manages item in your offhand",
    modulePriority = 2000
) {
    private val page = setting(this, EnumSetting(settingName("Page"), Page.GENERAL))
    private val priority by setting(this, EnumSetting(settingName("Priority"), Priority.INVENTORY, { page.value == Page.GENERAL }))
    private val switchMessage by setting(this, BooleanSetting(settingName("Switch Message"), false, { page.value == Page.GENERAL }))
    private val delay by setting(this, IntegerSetting(settingName("Delay"), 1, 1..20, 1, { page.value == Page.GENERAL }, "Ticks to wait between each move"))
    private val confirmTimeout by setting(this, IntegerSetting(settingName("Confirm Timeout"), 2, 0..20, 1, { page.value == Page.GENERAL }, "Maximum ticks to wait for confirm packets from server"))

    private val totemMode by setting(this, EnumSetting(settingName("Totem Mode"), TotemMode.HEALTH, { page.value == Page.TOTEM }))
    private val totemBind by setting(this, BindSetting(settingName("Totem Bind"), Bind(), null, { page.value == Page.TOTEM }))
    private val staticHealth by setting(this, FloatSetting(settingName("Static Health"), 6.0f, 1.0f..20.0f, 0.5f, { page.value == Page.TOTEM && totemMode.value == TotemMode.HEALTH }, "Always use totem below this health"))
    private val staticHp by setting(this, FloatSetting(settingName("Health"), 12.0f, 1.0f..20.0f, 0.5f, { page.value == Page.TOTEM }))
    private val damageHp by setting(this, FloatSetting(settingName("Damage Health"), 4.0f, 1.0f..20.0f, 0.5f, { page.value == Page.TOTEM && totemMode.value == TotemMode.HEALTH }))
    private val damageTimeout by setting(this, IntegerSetting(settingName("Damage Timeout"), 100, 10..1000, 10, { page.value == Page.TOTEM && totemMode.value == TotemMode.HEALTH }))
    private val falling by setting(this, BooleanSetting(settingName("Falling"), true, { page.value == Page.TOTEM && totemMode.value == TotemMode.HEALTH }))
    private val mob by setting(this, BooleanSetting(settingName("Mob"), true, { page.value == Page.TOTEM && totemMode.value == TotemMode.HEALTH }))
    private val checkPlayer by setting(this, BooleanSetting(settingName("Player"), true, { page.value == Page.TOTEM && totemMode.value == TotemMode.HEALTH }))
    private val arrow by setting(this, BooleanSetting(settingName("Arrow"), true, { page.value == Page.TOTEM && totemMode.value == TotemMode.HEALTH }))
    private val crystal0 = setting(this, BooleanSetting(settingName("Crystal"), true, { page.value == Page.TOTEM && totemMode.value == TotemMode.HEALTH }))
    private val crystalBias by setting(this, FloatSetting(settingName("Crystal Bias"), 1.1f, 0.0f..2.0f, 0.05f, { page.value == Page.TOTEM && crystal0.value && totemMode.value == TotemMode.HEALTH }))
    private val mainHandMode by setting(this, EnumSetting(settingName("Main Hand Mode"), MainHandMode.OFF, { page.value == Page.TOTEM }))
    private val swapMode by setting(this, EnumSetting(settingName("Swap Mode"), SwapMode.NORMAL, { page.value == Page.TOTEM && mainHandMode.value != MainHandMode.OFF }))
    private val ghostSwitchBypass by setting(this, BooleanSetting(settingName("Ghost Switch Bypass"), true, { page.value == Page.TOTEM && mainHandMode.value != MainHandMode.OFF && swapMode.value == SwapMode.SILENT }))

    private val crystalMode by setting(this, EnumSetting(settingName("Crystal Mode"), CrystalMode.DISABLED, { page.value == Page.CRYSTAL }))
    private val crystalBind by setting(this, BindSetting(settingName("Crystal Bind"), Bind(), null, { page.value == Page.CRYSTAL }))
    private val crystalHealth by setting(this, FloatSetting(settingName("Health"), 18.0f, 1.0f..20.0f, 0.5f, { page.value == Page.CRYSTAL && crystalMode.value == CrystalMode.HEALTH }))

    private val gappleMode by setting(this, EnumSetting(settingName("Gapple Mode"), GappleMode.DISABLED, { page.value == Page.GAPPLE }))
    private val gappleBind by setting(this, BindSetting(settingName("Gapple Bind"), Bind(), null, { page.value == Page.GAPPLE }))
    private val gappleHealth by setting(this, FloatSetting(settingName("Health"), 16.0f, 1.0f..20.0f, 0.5f, { page.value == Page.GAPPLE && gappleMode.value == GappleMode.HEALTH }))
    private val checkRightClick by setting(this, BooleanSetting(settingName("Right Click"), true, { page.value == Page.GAPPLE && gappleMode.value == GappleMode.CHECK }))
    private val rightClickWeapon by setting(this, BooleanSetting(settingName("Right Click Weapon"), false, { page.value == Page.GAPPLE && gappleMode.value == GappleMode.CHECK && checkRightClick }))
    private val checkBedAuraG by setting(this, BooleanSetting(settingName("Check Bed Aura"), true, { page.value == Page.GAPPLE && gappleMode.value == GappleMode.CHECK }))
    private val checkCrystalAuraG by setting(this, BooleanSetting(settingName("Check Crystal Aura"), true, { page.value == Page.GAPPLE && gappleMode.value == GappleMode.CHECK }))
    private val checkWeaponG by setting(this, BooleanSetting(settingName("Check Weapon"), true, { page.value == Page.GAPPLE && gappleMode.value == GappleMode.CHECK }))
    private val pickaxeWeaponG by setting(this, BooleanSetting(settingName("Pickaxe is Weapon"), false, { page.value == Page.GAPPLE && gappleMode.value == GappleMode.CHECK && checkWeaponG }))

    private val strengthMode by setting(this, EnumSetting(settingName("Strength Mode"), StrengthMode.DISABLED, { page.value == Page.STRENGTH }))
    private val strengthBind by setting(this, BindSetting(settingName("Strength Bind"), Bind(), null, { page.value == Page.STRENGTH }))
    private val strengthHealth by setting(this, FloatSetting(settingName("Health"), 18.0f, 1.0f..20.0f, 0.5f, { page.value == Page.STRENGTH && strengthMode.value == StrengthMode.HEALTH }))
    private val checkKillAuraS by setting(this, BooleanSetting(settingName("Check Kill Aura"), true, { page.value == Page.STRENGTH && strengthMode.value == StrengthMode.CHECK }))
    private val checkWeaponS by setting(this, BooleanSetting(settingName("Check Weapon"), true, { page.value == Page.STRENGTH && strengthMode.value == StrengthMode.CHECK }))
    private val pickaxeWeaponS by setting(this, BooleanSetting(settingName("Pickaxe is Weapon"), false, { page.value == Page.STRENGTH && strengthMode.value == StrengthMode.CHECK && checkWeaponS }))
    private val checkStrengthS by setting(this, BooleanSetting(settingName("Check Strength"), true, { page.value == Page.STRENGTH && strengthMode.value == StrengthMode.CHECK }))

    private val damageTimer = TickTimer()
    private var lastDamage = 0.0f
    private var lastTask: InventoryTask? = null
    private var lastType: ItemType? = null
    private var totemBindActive = false
    private var crystalBindActive = false
    private var gappleBindActive = false
    private var strengthBindActive = false

    init {
        onDisable {
            damageTimer.reset(-69420L)
            lastDamage = 0.0f
            lastType = null
            totemBindActive = false
            crystalBindActive = false
            gappleBindActive = false
            strengthBindActive = false
        }

        listener<InputEvent.Keyboard> {
            if (!it.state) return@listener
            if (totemBind.value.isDown(it.key)) {
                totemBindActive = !totemBindActive
                crystalBindActive = false; gappleBindActive = false; strengthBindActive = false
                if (switchMessage) NoSpamMessage.sendMessage("${getChatName()} Totem bind ${if (totemBindActive) "enabled" else "disabled"}")
            }
            if (crystalBind.value.isDown(it.key)) {
                crystalBindActive = !crystalBindActive
                totemBindActive = false; gappleBindActive = false; strengthBindActive = false
                if (switchMessage) NoSpamMessage.sendMessage("${getChatName()} Crystal bind ${if (crystalBindActive) "enabled" else "disabled"}")
            }
            if (gappleBind.value.isDown(it.key)) {
                gappleBindActive = !gappleBindActive
                totemBindActive = false; crystalBindActive = false; strengthBindActive = false
                if (switchMessage) NoSpamMessage.sendMessage("${getChatName()} Gapple bind ${if (gappleBindActive) "enabled" else "disabled"}")
            }
            if (strengthBind.value.isDown(it.key)) {
                strengthBindActive = !strengthBindActive
                totemBindActive = false; crystalBindActive = false; gappleBindActive = false
                if (switchMessage) NoSpamMessage.sendMessage("${getChatName()} Strength bind ${if (strengthBindActive) "enabled" else "disabled"}")
            }
        }

        listener<CrystalSpawnEvent> {
            if (totemMode.value != TotemMode.HEALTH || !crystal0.value) return@listener
            val damage = it.crystalDamage.selfDamage.pow(crystalBias)
            synchronized(damageTimer) {
                if (damage >= lastDamage) {
                    lastDamage = damage
                    damageTimer.reset()
                    runSafe {
                        if (checkTotem(this)) switchToType(this, ItemType.TOTEM)
                    }
                }
            }
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (player.isDead || player.health <= 0.0f || (lastTask != null && !lastTask!!.confirmed)) return@safeListener

            DefaultScope.launch {
                if (totemMode.value == TotemMode.HEALTH) updateDamage(this@safeListener)
                switchToType(this@safeListener, getType(this@safeListener))
            }
        }
    }

    override fun getHudInfo(): String = lastType?.name?.toLowerCase(Locale.ROOT) ?: ""

    private fun getType(event: SafeClientEvent): ItemType? {
        if (totemBindActive) return ItemType.TOTEM
        if (crystalBindActive && checkTotem(event)) return ItemType.CRYSTAL
        if (gappleBindActive && checkTotem(event)) return ItemType.GAPPLE
        if (strengthBindActive && checkTotem(event)) return ItemType.STRENGTH

        return when {
            checkTotem(event) -> ItemType.TOTEM
            checkCrystal(event) -> ItemType.CRYSTAL
            checkGapple(event) -> ItemType.GAPPLE
            checkStrength(event) -> ItemType.STRENGTH
            event.player.heldItemOffhand.isEmpty -> ItemType.TOTEM
            else -> null
        }
    }

    private fun checkTotem(event: SafeClientEvent): Boolean {
        return when (totemMode.value) {
            TotemMode.ALWAYS -> true
            TotemMode.HEALTH -> {
                val hp = CombatUtils.getScaledHealth(event.player)
                hp < staticHealth || hp < staticHp || hp - lastDamage <= damageHp
            }
        }
    }

    private fun checkCrystal(event: SafeClientEvent): Boolean {
        return when (crystalMode.value) {
            CrystalMode.DISABLED -> false
            CrystalMode.HEALTH -> CombatUtils.getScaledHealth(event.player) >= crystalHealth
            CrystalMode.CRYSTALAURA -> CrystalPlaceBreak.isActive() && CombatManager.isOnTopPriority(CrystalPlaceBreak)
        }
    }

    private fun checkGapple(event: SafeClientEvent): Boolean {
        return when (gappleMode.value) {
            GappleMode.DISABLED -> false
            GappleMode.HEALTH -> CombatUtils.getScaledHealth(event.player) >= gappleHealth
            GappleMode.CHECK -> {
                val rightClick = if (checkRightClick) {
                    val useKey = event.mc.gameSettings.keyBindUseItem.isKeyDown
                    val notUsable = !event.player.heldItemMainhand.isEmpty && !isUsableItem(event.player.heldItemMainhand)
                    val weaponCheck = !rightClickWeapon || isWeapon(event.player.heldItemMainhand, pickaxeWeaponG)
                    useKey && notUsable && weaponCheck
                } else false

                val bedAura = checkBedAuraG && BedAura.isEnabled && CombatManager.isOnTopPriority(BedAura)
                val crystalAura = checkCrystalAuraG && CrystalPlaceBreak.isActive() && CombatManager.isOnTopPriority(CrystalPlaceBreak)
                val weapon = checkWeaponG && isWeapon(event.player.heldItemMainhand, pickaxeWeaponG)

                rightClick || bedAura || crystalAura || weapon
            }
        }
    }

    private fun checkStrength(event: SafeClientEvent): Boolean {
        if (checkStrengthS && event.player.isPotionActive(MobEffects.STRENGTH)) return false
        if (event.player.inventoryContainer.inventory.none { ItemType.STRENGTH.filter(it) }) return false

        return when (strengthMode.value) {
            StrengthMode.DISABLED -> false
            StrengthMode.HEALTH -> CombatUtils.getScaledHealth(event.player) >= strengthHealth
            StrengthMode.CHECK -> {
                val ka = checkKillAuraS && CombatManager.isActiveAndTopPriority(KillAura)
                val weapon = checkWeaponS && isWeapon(event.player.heldItemMainhand, pickaxeWeaponS)
                ka || weapon
            }
        }
    }

    private fun isUsableItem(stack: ItemStack): Boolean {
        val item = stack.item
        return item == Items.GOLDEN_APPLE || item == Items.POTIONITEM || item == Items.EXPERIENCE_BOTTLE || item == Items.BOW || item.getItemUseAction(stack) != EnumAction.NONE
    }

    private fun isWeapon(stack: ItemStack, pickaxeIsWeapon: Boolean): Boolean {
        val item = stack.item
        return ItemKt.isWeapon(item) || (pickaxeIsWeapon && item is ItemPickaxe)
    }

    private fun switchToType(event: SafeClientEvent, type: ItemType?) {
        if (Suicide.isActive()) return
        if (type == null) {
            lastType = null
            return
        }
        if (checkOffhandItem(event, type)) return

        if (type == ItemType.TOTEM && mainHandMode.value != MainHandMode.OFF) {
            handleMainHandTotem(event)
            return
        }

        val slot = getItemSlot(event, type) ?: return
        if (slot == DefinedKt.getOffhandSlot(event.player)) return

        ThreadSafetyKt.onMainThread {
            InventoryTask.Builder().apply {
                priority(Int.MAX_VALUE)
                postDelay(delay.toLong(), TimeUnit.TICKS)
                timeout(confirmTimeout.toLong(), TimeUnit.TICKS)
                moveTo(slot, DefinedKt.getOffhandSlot(event.player))
            }.build().let {
                InventoryTaskManager.runNow(event, it)
                lastTask = it
            }
        }
        lastType = type
        if (switchMessage) NoSpamMessage.sendMessage("${getChatName()} Offhand now has a ${type.name.toLowerCase(Locale.ROOT)}")
    }

    private fun handleMainHandTotem(event: SafeClientEvent) {
        val mode = mainHandMode.value
        if (mode == MainHandMode.REPLACE) {
            val totemSlot = DefinedKt.getHotbarSlots(event.player).firstOrNull { it.stack.item == Items.TOTEM_OF_UNDYING } ?: return
            if (event.player.heldItemMainhand.item != Items.TOTEM_OF_UNDYING) {
                when (swapMode.value) {
                    SwapMode.NORMAL -> {
                        event.player.connection.sendPacket(CPacketHeldItemChange(totemSlot.hotbarIndex))
                        event.player.inventory.currentItem = totemSlot.hotbarIndex
                    }
                    SwapMode.SILENT -> {
                        if (ghostSwitchBypass) performGhostSwitchBypass(event)
                        HotbarSwitchManager.ghostSwitch(event, HotbarSwitchManager.Override.NONE, totemSlot) {}
                    }
                }
            } else {
                synchronized(InventoryTaskManager) {
                    event.connection.sendPacket(CPacketHeldItemChange(event.player.inventory.currentItem))
                }
            }
        } else if (mode == MainHandMode.SWAP) {
            val totemSlot = getItemSlot(event, ItemType.TOTEM) ?: return
            if (event.player.heldItemMainhand.item != Items.TOTEM_OF_UNDYING) {
                when (swapMode.value) {
                    SwapMode.NORMAL -> {
                        if (totemSlot is HotbarSlot) {
                            event.player.connection.sendPacket(CPacketHeldItemChange(totemSlot.hotbarIndex))
                            event.player.inventory.currentItem = totemSlot.hotbarIndex
                        }
                    }
                    SwapMode.SILENT -> {
                        if (ghostSwitchBypass) performGhostSwitchBypass(event)
                        HotbarSwitchManager.ghostSwitch(event, HotbarSwitchManager.Override.NONE, totemSlot) {}
                    }
                }
            }
        }
        lastType = ItemType.TOTEM
        if (switchMessage) NoSpamMessage.sendMessage("${getChatName()} Main hand now has a totem")
    }

    private fun performGhostSwitchBypass(event: SafeClientEvent) {
        val x = floor(event.player.posX).toInt()
        val z = floor(event.player.posZ).toInt()
        val obbySlot = IterableKt.firstItem(DefinedKt.getAllSlotsPrioritized(event.player), Item.getItemFromBlock(Blocks.OBSIDIAN)) ?: return
        HotbarSwitchManager.ghostSwitch(event, obbySlot) {
            event.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(BlockPos(x, 0, z), EnumFacing.DOWN, EnumHand.MAIN_HAND, x.toFloat(), -1.0f, z.toFloat()))
        }
    }

    private fun checkOffhandItem(event: SafeClientEvent, type: ItemType): Boolean = type.filter(event.player.heldItemOffhand)

    private fun getItemSlot(event: SafeClientEvent, type: ItemType): Slot? {
        val offhand = DefinedKt.getOffhandSlot(event.player)
        if (type.filter(offhand.stack)) return offhand

        return if (priority == Priority.HOTBAR) {
            findItemByType(DefinedKt.getHotbarSlots(event.player), type) ?: findItemByType(DefinedKt.getInventorySlots(event.player), type) ?: findItemByType(DefinedKt.getCraftingSlots(event.player), type)
        } else {
            findItemByType(DefinedKt.getInventorySlots(event.player), type) ?: findItemByType(DefinedKt.getHotbarSlots(event.player), type) ?: findItemByType(DefinedKt.getCraftingSlots(event.player), type)
        }
    }

    private fun findItemByType(slots: List<Slot>, type: ItemType): Slot? = slots.firstOrNull { type.filter(it.stack) }

    private fun updateDamage(event: SafeClientEvent) {
        var maxDamage = 0.0f
        if (mob) maxDamage = maxOf(getMobDamage(event), maxDamage)
        if (checkPlayer) maxDamage = maxOf(getPlayerDamage(event), maxDamage)
        if (arrow) maxDamage = maxOf(getArrowDamage(event), maxDamage)
        if (crystal0.value) maxDamage = maxOf(getCrystalDamage(), maxDamage)
        if (falling) {
            val dist = getNextFallDist(event)
            if (dist > 3.0f) maxDamage = maxOf(ceil(dist - 3.0f), maxDamage)
        }

        synchronized(damageTimer) {
            if (maxDamage >= lastDamage) {
                lastDamage = maxDamage
                damageTimer.reset()
            } else if (damageTimer.tick(damageTimeout.value.toLong())) {
                lastDamage = maxDamage
            }
        }
    }

    private fun getMobDamage(event: SafeClientEvent): Float = EntityManager.livingBase.asSequence()
        .filterIsInstance<EntityMob>()
        .filter { event.player.getDistanceSq(it) <= 64.0 }
        .map { CombatUtils.calcDamageFromMob(event, it) }
        .maxOrNull() ?: 0.0f

    private fun getPlayerDamage(event: SafeClientEvent): Float = EntityManager.players.asSequence()
        .filter { !EntityUtils.isFakeOrSelf(it) && event.player.getDistanceSq(it) <= 64.0 }
        .map { CombatUtils.calcDamageFromPlayer(event, it, true) }
        .maxOrNull() ?: 0.0f

    private fun getArrowDamage(event: SafeClientEvent): Float {
        val rawDamage = EntityManager.entity.asSequence()
            .filterIsInstance<EntityArrow>()
            .filter { event.player.getDistanceSq(it) <= 250.0 }
            .map { arrow ->
                var d = ceil(MovementUtils.getRealSpeed(arrow) * arrow.damage).toFloat()
                if (arrow.getIsCritical) d = d * 0.5f + 1.0f
                if (arrow is EntityTippedArrow) d += getTippedArrowDamage(arrow)
                d
            }.maxOrNull() ?: 0.0f

        return CombatManager.getDamageReduction(event.player)?.calcDamage(rawDamage, false) ?: rawDamage
    }

    private fun getTippedArrowDamage(arrow: EntityTippedArrow): Float {
        val effect = PotionUtils.getEffectsFromStack(arrow.potion).firstOrNull { it.potion == MobEffects.INSTANT_DAMAGE }
        return if (effect != null) 3.0f * 2.0f.pow(effect.amplifier + 1) else 0.0f
    }

    private fun getCrystalDamage(): Float = CombatManager.crystalList.asSequence()
        .map { it.second.selfDamage.pow(crystalBias) }
        .maxOrNull() ?: 0.0f

    private fun getNextFallDist(event: SafeClientEvent): Float = event.player.fallDistance - (event.player.posY - event.player.prevPosY).toFloat()

    private enum class CrystalMode(override val displayName: CharSequence) : DisplayEnum { HEALTH("Health"), CRYSTALAURA("Crystal Aura"), DISABLED("Disabled") }
    private enum class GappleMode(override val displayName: CharSequence) : DisplayEnum { HEALTH("Health"), CHECK("Check"), DISABLED("Disabled") }
    private enum class ItemType(val filter: (ItemStack) -> Boolean) {
        TOTEM({ it.item == Items.TOTEM_OF_UNDYING }),
        GAPPLE({ it.item == Items.GOLDEN_APPLE }),
        STRENGTH({ stack -> stack.item is ItemPotion && PotionUtils.getEffectsFromStack(stack).any { it.potion == MobEffects.STRENGTH } }),
        CRYSTAL({ it.item == Items.END_CRYSTAL })
    }
    private enum class MainHandMode(override val displayName: CharSequence) : DisplayEnum { SWAP("Swap"), REPLACE("Replace"), OFF("Off") }
    private enum class Page(override val displayName: CharSequence) : DisplayEnum { GENERAL("General"), TOTEM("Totem"), CRYSTAL("Crystal"), GAPPLE("Gapple"), STRENGTH("Strength") }
    private enum class Priority { INVENTORY, HOTBAR }
    private enum class StrengthMode(override val displayName: CharSequence) : DisplayEnum { HEALTH("Health"), CHECK("Check"), DISABLED("Disabled") }
    private enum class SwapMode(override val displayName: CharSequence) : DisplayEnum { NORMAL("Normal"), SILENT("Silent") }
    private enum class TotemMode(override val displayName: CharSequence) : DisplayEnum { ALWAYS("Always"), HEALTH("Health") }
}
