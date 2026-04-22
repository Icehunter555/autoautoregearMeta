package dev.wizard.meta

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import org.apache.logging.log4j.LogManager
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins

@IFMLLoadingPlugin.Name("MetaCoreMod")
@IFMLLoadingPlugin.MCVersion("1.12.2")
class MetaCoreMod : IFMLLoadingPlugin {
    init {
        MixinBootstrap.init()
        Mixins.addConfigurations("mixins.meta.core.json", "mixins.meta.accessor.json", "mixins.meta.patch.json", "mixins.baritone.json")
        MixinEnvironment.getDefaultEnvironment().obfuscationContext = "searge"
        LogManager.getLogger("Meta").info("Meta and Baritone mixins initialised.")
    }

    override fun injectData(data: Map<String, Any>) {
    }

    override fun getASMTransformerClass(): Array<String> {
        return emptyArray()
    }

    override fun getModContainerClass(): String? {
        return null
    }

    override fun getSetupClass(): String? {
        return null
    }

    override fun getAccessTransformerClass(): String? {
        return null
    }
}