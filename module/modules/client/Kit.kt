package dev.wizard.meta.module.modules.client

import com.google.gson.reflect.TypeToken
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.collection.MapSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.inventory.regName
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import java.util.*

object Kit : Module(
    "Kit",
    category = Category.CLIENT,
    description = "Setting up kit management",
    alwaysEnabled = true
) {
    val kitMap = setting(this, MapSetting(
        settingName("Kit"),
        TreeMap<String, List<String>>(),
        object : TypeToken<TreeMap<String, List<String>>>() {},
        LambdaUtilsKt.BOOLEAN_SUPPLIER_FALSE,
        ""
    ))

    var kitName by setting(this, StringSetting(settingName("Kit Name"), "None"))

    fun getKitItemArray(): Array<ItemEntry>? {
        val treeMap = kitMap.value
        val name = kitName.toLowerCase(Locale.ROOT)
        val stringList = treeMap[name] ?: return null

        return Array(36) { i ->
            val string = stringList.getOrNull(i)
            if (string != null) {
                ItemEntry.fromString(string)
            } else {
                ItemEntry.EMPTY
            }
        }
    }

    data class ItemEntry(val item: Item, val name: String?) {
        override fun toString(): String = "${item.regName} ${name ?: ""}".trim()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return when (other) {
                is ItemEntry -> item == other.item && (name == null || name == other.name)
                is ItemStack -> item == other.item && (name == null || name == other.displayName)
                is Item -> item == other
                else -> false
            }
        }

        override fun hashCode(): Int {
            var result = item.hashCode()
            result = 31 * result + (name?.hashCode() ?: 0)
            return result
        }

        companion object {
            val EMPTY = ItemEntry(Items.AIR, null)

            fun fromString(string: String): ItemEntry {
                val index = string.indexOf(' ')
                val itemRegName = if (index == -1) string else string.substring(0, index)
                val item = Item.getByNameOrId(itemRegName) ?: Items.AIR
                val name = if (index == -1) null else string.substring(index + 1)
                return ItemEntry(item, name)
            }

            fun fromStack(stack: ItemStack): ItemEntry {
                return ItemEntry(stack.item, stack.displayName)
            }
        }
    }
}
