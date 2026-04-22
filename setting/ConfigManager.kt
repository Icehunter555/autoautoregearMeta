package dev.wizard.meta.setting

import dev.wizard.meta.MetaMod
import dev.wizard.meta.setting.configs.IConfig
import dev.wizard.meta.util.collections.NameableSet

object ConfigManager {
    private val configSet = NameableSet<IConfig>()

    init {
        register(GuiConfig.INSTANCE)
        register(ModuleConfig.INSTANCE)
    }

    fun loadAll(): Boolean {
        var success = load(GenericConfig.INSTANCE)
        configSet.forEach {
            success = load(it) || success
        }
        return success
    }

    fun load(config: IConfig): Boolean {
        return try {
            config.load()
            MetaMod.logger.info("${config.name} config loaded")
            true
        } catch (e: Exception) {
            MetaMod.logger.error("Failed to load ${config.name} config", e)
            false
        }
    }

    fun saveAll(): Boolean {
        var success = save(GenericConfig.INSTANCE)
        configSet.forEach {
            success = save(it) || success
        }
        return success
    }

    fun save(config: IConfig): Boolean {
        return try {
            config.save()
            MetaMod.logger.info("${config.name} config saved")
            true
        } catch (e: Exception) {
            MetaMod.logger.error("Failed to save ${config.name} config!", e)
            false
        }
    }

    fun register(config: IConfig) {
        configSet.add(config)
    }

    fun unregister(config: IConfig) {
        configSet.remove(config)
    }
}
