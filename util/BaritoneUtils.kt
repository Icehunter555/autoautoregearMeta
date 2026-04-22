package dev.wizard.meta.util

import baritone.api.BaritoneAPI
import baritone.api.IBaritone
import baritone.api.IBaritoneProvider
import baritone.api.Settings
import com.google.common.collect.ImmutableSet
import net.minecraft.block.Block
import net.minecraft.init.Blocks

object BaritoneUtils {
    var initialized: Boolean = false
    val baritoneCachedBlocks: ImmutableSet<Block>

    val provider: IBaritoneProvider?
        get() = if (initialized) BaritoneAPI.getProvider() else null

    val settings: Settings?
        get() = if (initialized) BaritoneAPI.getSettings() else null

    val primary: IBaritone?
        get() = provider?.primaryBaritone

    val prefix: String
        get() = settings?.prefix?.value ?: "#"

    val isPathing: Boolean
        get() = primary?.pathingBehavior?.isPathing ?: false

    val isActive: Boolean
        get() = (primary?.customGoalProcess?.isActive ?: false) ||
                (primary?.pathingControlManager?.mostRecentInControl()?.orElse(null)?.isActive ?: false)

    fun cancelEverything(): Boolean? {
        return primary?.pathingBehavior?.cancelEverything()
    }

    init {
        val blocks = arrayOf(
            Blocks.field_150477_bB, Blocks.field_150460_al, Blocks.field_150486_ae, Blocks.field_150447_bR,
            Blocks.field_150384_bq, Blocks.field_150378_br, Blocks.field_150474_ac, Blocks.field_180401_cv,
            Blocks.field_190976_dk, Blocks.field_190977_dl, Blocks.field_190978_dm, Blocks.field_190979_dn,
            Blocks.field_190980_do, Blocks.field_190981_dp, Blocks.field_190982_dq, Blocks.field_190983_dr,
            Blocks.field_190984_ds, Blocks.field_190985_dt, Blocks.field_190986_du, Blocks.field_190987_dv,
            Blocks.field_190988_dw, Blocks.field_190989_dx, Blocks.field_190990_dy, Blocks.field_190991_dz,
            Blocks.field_190975_dA, Blocks.field_150427_aO, Blocks.field_150438_bZ, Blocks.field_150461_bJ,
            Blocks.field_150382_bo, Blocks.field_150465_bP, Blocks.field_150381_bn, Blocks.field_150467_bQ,
            Blocks.field_150470_am, Blocks.field_150324_C, Blocks.field_150380_bt, Blocks.field_150421_aI,
            Blocks.field_185775_db, Blocks.field_150321_G, Blocks.field_150388_bm, Blocks.field_150468_ap,
            Blocks.field_150395_bd
        )
        baritoneCachedBlocks = ImmutableSet.of(
            Blocks.field_150484_ah, Blocks.field_150402_ci, Blocks.field_150339_S, Blocks.field_150340_R,
            Blocks.field_150412_bA, Blocks.field_150475_bE, *blocks
        )
    }
}