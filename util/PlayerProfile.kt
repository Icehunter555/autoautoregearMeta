package dev.wizard.meta.util

import java.util.UUID

class PlayerProfile(val uuid: UUID, val name: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerProfile) return false
        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    fun isInvalid(): Boolean {
        return uuid == INVALID.uuid
    }

    companion object {
        @JvmField
        val INVALID = PlayerProfile(UUID(0L, 0L), "Invalid")
    }
}
