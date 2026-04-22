package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.FolderUtils

object FolderCommand : ClientCommand("folder", arrayOf("open"), "open a folder") {

    init {
        executeSafe("Opens the trollhack folder") {
            FolderUtils.openFolder(FolderUtils.trollFolder)
        }

        literal("resourcepack") {
            executeSafe("Opens the resource packs folder") {
                FolderUtils.openFolder(FolderUtils.resourcepackFolder)
            }
        }

        literal("mods") {
            executeSafe("Opens the mods folder") {
                FolderUtils.openFolder(FolderUtils.modsFolder)
            }
        }

        literal("logs") {
            executeSafe("Opens the logs folder") {
                FolderUtils.openFolder(FolderUtils.logFolder)
            }
        }

        literal("modconfig") {
            executeSafe("Opens the forge config folder") {
                FolderUtils.openFolder(FolderUtils.mcConfigFolder)
            }
        }

        literal("config") {
            executeSafe("Opens the config folder") {
                FolderUtils.openFolder(FolderUtils.moduleFolder)
            }
        }

        literal("screenshot") {
            executeSafe("Opens the screenshots folder") {
                FolderUtils.openFolder(FolderUtils.screenshotFolder)
            }
        }
    }
}
