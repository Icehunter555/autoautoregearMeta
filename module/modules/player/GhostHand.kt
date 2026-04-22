package dev.wizard.meta.module.modules.player

import com.google.gson.reflect.TypeToken
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.collection.CollectionSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.math.vector.toBlockPos
import dev.wizard.meta.util.world.RayTraceAction
import dev.wizard.meta.util.world.RaytraceKt
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*

object GhostHand : Module(
    "Ghost Hand",
    category = Category.PLAYER,
    description = "Ignores interaction with certain blocks"
) {
    private val defaultVisibleList = linkedSetOf("minecraft:bedrock", "minecraft:portal_frame", "minecraft:portal")
    private val ignoreListed by setting(this, BooleanSetting(settingName("Ignore Listed"), true))
    val blockList = setting(this, CollectionSetting(settingName("Block List"), defaultVisibleList, object : TypeToken<LinkedHashSet<String>>() {}.type))

    private val function = { _: net.minecraft.world.World, _: net.minecraft.util.math.BlockPos, state: net.minecraft.block.state.IBlockState ->
        val block = state.block
        if (block == Blocks.AIR) {
            RayTraceAction.Skip
        } else if (block.canCollideCheck(state, false)) {
            if (ignoreListed != blockList.contains(block.registryName.toString())) {
                RayTraceAction.Calc
            } else {
                RayTraceAction.Skip
            }
        } else {
            RayTraceAction.Skip
        }
    }

    @JvmStatic
    fun handleRayTrace(blockReachDistance: Double, partialTicks: Float, cir: CallbackInfoReturnable<RayTraceResult>) {
        if (INSTANCE.isDisabled()) return

        if (mc.currentScreen == null) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) return
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return
            if (Mouse.isButtonDown(1)) return
        }

        val player = mc.player ?: return
        val eyePos = EntityUtils.getEyePosition(player)
        val lookVec = player.getLook(partialTicks)
        val sightEnd = eyePos.add(lookVec.x * blockReachDistance, lookVec.y * blockReachDistance, lookVec.z * blockReachDistance)

        var result = RaytraceKt.rayTrace(player.world, eyePos, sightEnd, 50, function)
        if (result == null) {
            result = RayTraceResult(RayTraceResult.Type.MISS, sightEnd, EnumFacing.UP, sightEnd.toBlockPos())
        }
        cir.returnValue = result
    }
}
