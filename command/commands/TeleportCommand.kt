package dev.wizard.meta.command.commands

import dev.wizard.meta.command.BlockPosArg
import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.Vec3d

object TeleportCommand : ClientCommand("teleport", arrayOf("tp"), "Teleport exploit from popbob") {

    private fun SafeClientEvent.teleport(x: Int, y: Int, z: Int) {
        teleport(x.toDouble() + 0.5, y.toDouble(), z.toDouble() + 0.5)
    }

    private fun SafeClientEvent.teleport(x: Double, y: Double, z: Double) {
        player.setPosition(x, y, z)
        connection.sendPacket(CPacketPlayer.Position(x, y, z, player.onGround))
    }

    private fun SafeClientEvent.teleport(x: Int, y: Int, z: Int, speed: Double) {
        teleport(x.toDouble() + 0.5, y.toDouble(), z.toDouble() + 0.5, speed)
    }

    private fun SafeClientEvent.teleport(x: Double, y: Double, z: Double, speed: Double) {
        if (speed.isNaN()) {
            teleport(x, y, z)
        } else {
            val playerPos = player.positionVector
            val rotation = RotationUtils.getRotationTo(playerPos, Vec3d(x, y, z))
            val dirVec = Vec3d.fromPitchYaw(Vec2f.getY(rotation), Vec2f.getX(rotation)).scale(speed)

            var currentX = player.posX
            var currentY = player.posY
            var currentZ = player.posZ

            val xGreater = x >= currentX
            val yGreater = y >= currentY
            val zGreater = z >= currentZ

            do {
                currentX = addOrSubtractBounded(xGreater, currentX, dirVec.x, x)
                currentY = addOrSubtractBounded(yGreater, currentY, dirVec.y, y)
                currentZ = addOrSubtractBounded(zGreater, currentZ, dirVec.z, z)
                player.setPosition(currentX, currentY, currentZ)
                connection.sendPacket(CPacketPlayer.Position(currentX, currentY, currentZ, player.onGround))
            } while (currentX != x || currentY != y || currentZ != z)
        }
    }

    private fun addOrSubtractBounded(greater: Boolean, a: Double, b: Double, bound: Double): Double {
        return if (greater) Math.min(a + b, bound) else Math.max(a + b, bound)
    }

    init {
        blockPos("pos") { posArg ->
            double("speed") { speedArg ->
                executeSafe {
                    val blockPos = getValue(posArg)
                    teleport(blockPos.x, blockPos.y, blockPos.z, getValue(speedArg))
                }
            }

            executeSafe {
                val blockPos = getValue(posArg)
                teleport(blockPos.x, blockPos.y, blockPos.z)
            }
        }

        int("x") { xArg ->
            int("y") { yArg ->
                int("z") { zArg ->
                    double("speed") { speedArg ->
                        executeSafe {
                            teleport(getValue(xArg), getValue(yArg), getValue(zArg), getValue(speedArg))
                        }
                    }

                    executeSafe {
                        teleport(getValue(xArg), getValue(yArg), getValue(zArg))
                    }
                }
            }
        }

        double("x") { xArg ->
            double("y") { yArg ->
                double("z") { zArg ->
                    double("speed") { speedArg ->
                        executeSafe {
                            teleport(getValue(xArg), getValue(yArg), getValue(zArg), getValue(speedArg))
                        }
                    }

                    executeSafe {
                        teleport(getValue(xArg), getValue(yArg), getValue(zArg))
                    }
                }
            }
        }

        int("x") { xArg ->
            int("z") { zArg ->
                double("speed") { speedArg ->
                    executeSafe {
                        teleport(getValue(xArg).toDouble() + 0.5, player.posY, getValue(zArg).toDouble() + 0.5, getValue(speedArg))
                    }
                }

                executeSafe {
                    teleport(getValue(xArg).toDouble() + 0.5, player.posY, getValue(zArg).toDouble() + 0.5)
                }
            }
        }

        double("x") { xArg ->
            double("z") { zArg ->
                double("speed") { speedArg ->
                    executeSafe {
                        teleport(getValue(xArg), player.posY, getValue(zArg), getValue(speedArg))
                    }
                }

                executeSafe {
                    teleport(getValue(xArg), player.posY, getValue(zArg))
                }
            }
        }
    }
}
