package dev.wizard.meta.command.commands

import dev.wizard.meta.command.BlockPosArg
import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.accessor.setId
import dev.wizard.meta.util.accessor.setPacketAction
import dev.wizard.meta.util.text.NoSpamMessage
import io.netty.buffer.Unpooled
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.EntityDonkey
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.*
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting

object SendPacketCommand : ClientCommand("sendpacket", description = "sends a packet") {

    private fun SafeClientEvent.deployPacket(packet: Packet<*>, info: String) {
        connection.networkManager.sendPacket(packet)
        val packetName = packet::class.java.name.split(".").lastOrNull() ?: "Unknown"
        NoSpamMessage.sendMessage("$chatName Sent ${TextFormatting.GRAY}$packetName${TextFormatting.DARK_RED} > ${TextFormatting.GRAY}$info")
    }

    init {
        literal("Animation") {
            enum<EnumHand>("hand") { handArg ->
                executeSafe {
                    deployPacket(CPacketAnimation(getValue(handArg)), getValue(handArg).toString())
                }
            }
        }

        literal("ChatMessage") {
            string("message") { messageArg ->
                executeSafe {
                    deployPacket(CPacketChatMessage(getValue(messageArg)), getValue(messageArg))
                }
            }
        }

        literal("ClientSettings") {
            string("lang") { langArg ->
                int("renderDistanceIn") { renderDistanceInArg ->
                    enum<EntityPlayer.EnumChatVisibility>("chatVisibilityIn") { chatVisibilityInArg ->
                        boolean("chatColorsIn") { chatColorsInArg ->
                            int("modelPartsIn") { modelPartsInArg ->
                                enum<EnumHandSide>("mainHandIn") { mainHandInArg ->
                                    executeSafe {
                                        val lang = getValue(langArg)
                                        val renderDistance = getValue(renderDistanceInArg)
                                        val chatVisibility = getValue(chatVisibilityInArg)
                                        val chatColors = getValue(chatColorsInArg)
                                        val modelParts = getValue(modelPartsInArg)
                                        val mainHand = getValue(mainHandInArg)
                                        deployPacket(
                                            CPacketClientSettings(lang, renderDistance, chatVisibility, chatColors, modelParts, mainHand),
                                            "$lang $renderDistance $chatVisibility $chatColors $modelParts $mainHand"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        literal("ClientStatus") {
            enum<CPacketClientStatus.State>("state") { stateArg ->
                executeSafe {
                    deployPacket(CPacketClientStatus(getValue(stateArg)), getValue(stateArg).toString())
                }
            }
        }

        literal("CloseWindow") {
            int("windowId") { windowIdArg ->
                executeSafe {
                    deployPacket(CPacketCloseWindow(getValue(windowIdArg)), getValue(windowIdArg).toString())
                }
            }
        }

        literal("ConfirmTeleport") {
            int("teleportId") { teleportIdArg ->
                executeSafe {
                    deployPacket(CPacketConfirmTeleport(getValue(teleportIdArg)), getValue(teleportIdArg).toString())
                }
            }
        }

        literal("CreativeInventoryAction") {
            int("slotId") { slotIdArg ->
                executeSafe {
                    deployPacket(CPacketCreativeInventoryAction(getValue(slotIdArg), ItemStack.EMPTY), "${getValue(slotIdArg)} ${ItemStack.EMPTY}")
                }
            }
        }

        literal("CustomPayload") {
            string("channel") { channelArg ->
                string("stringData") { dataArg ->
                    executeSafe {
                        val channel = getValue(channelArg)
                        val data = getValue(dataArg)
                        val buffer = PacketBuffer(Unpooled.buffer()).writeString(data)
                        deployPacket(CPacketCustomPayload(channel, buffer), "$channel $data")
                    }
                }
            }
        }

        literal("EnchantItem") {
            int("windowId") { windowIdArg ->
                int("button") { buttonArg ->
                    executeSafe {
                        deployPacket(CPacketEnchantItem(getValue(windowIdArg), getValue(buttonArg)), "${getValue(windowIdArg)} ${getValue(buttonArg)}")
                    }
                }
            }
        }

        literal("EntityAction") {
            enum<CPacketEntityAction.Action>("action") { actionArg ->
                int("auxData") { auxDataArg ->
                    executeSafe {
                        deployPacket(CPacketEntityAction(player, getValue(actionArg), getValue(auxDataArg)), "${player.entityId} ${getValue(actionArg)} ${getValue(auxDataArg)}")
                    }
                }
            }
        }

        literal("HeldItemChange") {
            int("slotId") { slotIdArg ->
                executeSafe {
                    deployPacket(CPacketHeldItemChange(getValue(slotIdArg)), getValue(slotIdArg).toString())
                }
            }
        }

        literal("Input") {
            float("strafeSpeed") { strafeSpeedArg ->
                float("forwardSpeed") { forwardSpeedArg ->
                    boolean("jumping") { jumpingArg ->
                        boolean("sneaking") { sneakingArg ->
                            executeSafe {
                                deployPacket(CPacketInput(getValue(strafeSpeedArg), getValue(forwardSpeedArg), getValue(jumpingArg), getValue(sneakingArg)), "${getValue(strafeSpeedArg)} ${getValue(forwardSpeedArg)} ${getValue(jumpingArg)} ${getValue(sneakingArg)}")
                            }
                        }
                    }
                }
            }
        }

        literal("KeepAlive") {
            long("key") { keyArg ->
                executeSafe {
                    deployPacket(CPacketKeepAlive(getValue(keyArg)), getValue(keyArg).toString())
                }
            }
        }

        literal("PlaceRecipe") {
            int("windowId") { windowIdArg ->
                string("recipe") { recipeArg ->
                    boolean("makeAll") { makeAllArg ->
                        executeSafe {
                            val recipeName = getValue(recipeArg)
                            val recipe = CraftingManager.REGISTRY.keys.find { it.toString() == recipeName }?.let { CraftingManager.REGISTRY.getObject(it) }
                            if (recipe != null) {
                                deployPacket(CPacketPlaceRecipe(getValue(windowIdArg), recipe, getValue(makeAllArg)), "${getValue(windowIdArg)} $recipeName ${getValue(makeAllArg)}")
                            }
                        }
                    }
                }
            }
        }

        literal("PlayerPosition") {
            double("x") { xArg ->
                double("y") { yArg ->
                    double("z") { zArg ->
                        boolean("onGround") { onGroundArg ->
                            executeSafe {
                                deployPacket(CPacketPlayer.Position(getValue(xArg), getValue(yArg), getValue(zArg), getValue(onGroundArg)), "${getValue(xArg)} ${getValue(yArg)} ${getValue(zArg)} ${getValue(onGroundArg)}")
                            }
                        }
                    }
                }
            }
        }

        literal("PlayerPositionRotation") {
            double("x") { xArg ->
                double("y") { yArg ->
                    double("z") { zArg ->
                        float("yaw") { yawArg ->
                            float("pitch") { pitchArg ->
                                boolean("onGround") { onGroundArg ->
                                    executeSafe {
                                        deployPacket(CPacketPlayer.PositionRotation(getValue(xArg), getValue(yArg), getValue(zArg), getValue(yawArg), getValue(pitchArg), getValue(onGroundArg)), "${getValue(xArg)} ${getValue(yArg)} ${getValue(zArg)} ${getValue(yawArg)} ${getValue(pitchArg)} ${getValue(onGroundArg)}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        literal("PlayerRotation") {
            float("yaw") { yawArg ->
                float("pitch") { pitchArg ->
                    boolean("onGround") { onGroundArg ->
                        executeSafe {
                            deployPacket(CPacketPlayer.Rotation(getValue(yawArg), getValue(pitchArg), getValue(onGroundArg)), "${getValue(yawArg)} ${getValue(pitchArg)} ${getValue(onGroundArg)}")
                        }
                    }
                }
            }
        }

        literal("PlayerDigging") {
            enum<CPacketPlayerDigging.Action>("action") { actionArg ->
                blockPos("position") { positionArg ->
                    enum<EnumFacing>("facing") { facingArg ->
                        executeSafe {
                            deployPacket(CPacketPlayerDigging(getValue(actionArg), getValue(positionArg), getValue(facingArg)), "${getValue(actionArg)} ${getValue(positionArg)} ${getValue(facingArg)}")
                        }
                    }
                }
            }
        }

        literal("PlayerTryUseItem") {
            enum<EnumHand>("hand") { handArg ->
                executeSafe {
                    deployPacket(CPacketPlayerTryUseItem(getValue(handArg)), getValue(handArg).toString())
                }
            }
        }

        literal("PlayerTryUseItemOnBlock") {
            blockPos("position") { positionArg ->
                enum<EnumFacing>("placedBlockDirection") { facingArg ->
                    enum<EnumHand>("hand") { handArg ->
                        float("facingX") { fxArg ->
                            float("facingY") { fyArg ->
                                float("facingZ") { fzArg ->
                                    executeSafe {
                                        deployPacket(CPacketPlayerTryUseItemOnBlock(getValue(positionArg), getValue(facingArg), getValue(handArg), getValue(fxArg), getValue(fyArg), getValue(fzArg)), "${getValue(positionArg)} ${getValue(facingArg)} ${getValue(handArg)} ${getValue(fxArg)} ${getValue(fyArg)} ${getValue(fzArg)}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        literal("ResourcePackStatus") {
            enum<CPacketResourcePackStatus.Action>("action") { actionArg ->
                executeSafe {
                    deployPacket(CPacketResourcePackStatus(getValue(actionArg)), getValue(actionArg).toString())
                }
            }
        }

        literal("SteerBoat") {
            boolean("left") { leftArg ->
                boolean("right") { rightArg ->
                    executeSafe {
                        deployPacket(CPacketSteerBoat(getValue(leftArg), getValue(rightArg)), "${getValue(leftArg)} ${getValue(rightArg)}")
                    }
                }
            }
        }

        literal("TabComplete") {
            string("message") { msgArg ->
                blockPos("targetBlock") { targetBlockArg ->
                    boolean("hasTargetBlock") { hasTargetBlockArg ->
                        executeSafe {
                            deployPacket(CPacketTabComplete(getValue(msgArg), getValue(targetBlockArg), getValue(hasTargetBlockArg)), "${getValue(msgArg)} ${getValue(targetBlockArg)} ${getValue(hasTargetBlockArg)}")
                        }
                    }
                }
            }
        }

        literal("UpdateSign") {
            blockPos("position") { positionArg ->
                string("line1") { l1Arg ->
                    string("line2") { l2Arg ->
                        string("line3") { l3Arg ->
                            string("line4") { l4Arg ->
                                executeSafe {
                                    val lines = arrayOf(
                                        TextComponentString(getValue(l1Arg)),
                                        TextComponentString(getValue(l2Arg)),
                                        TextComponentString(getValue(l3Arg)),
                                        TextComponentString(getValue(l4Arg))
                                    )
                                    deployPacket(CPacketUpdateSign(getValue(positionArg), lines), "${getValue(l1Arg)} ${getValue(l2Arg)} ${getValue(l3Arg)} ${getValue(l4Arg)}")
                                }
                            }
                        }
                    }
                }
            }
        }

        literal("UseEntityAttack") {
            int("ID") { idArg ->
                executeSafe {
                    val packet = CPacketUseEntity()
                    packet.setId(getValue(idArg))
                    packet.setPacketAction(CPacketUseEntity.Action.ATTACK)
                    deployPacket(packet, getValue(idArg).toString())
                }
            }
        }

        literal("UseEntityInteract") {
            enum<EnumHand>("hand") { handArg ->
                int("ID") { idArg ->
                    executeSafe {
                        val entity = EntityDonkey(world)
                        entity.entityId = getValue(idArg)
                        deployPacket(CPacketUseEntity(entity, getValue(handArg)), "${getValue(idArg)} ${getValue(handArg)}")
                    }
                }
            }
        }

        literal("UseEntityInteractAt") {
            enum<EnumHand>("hand") { handArg ->
                double("x") { xArg ->
                    double("y") { yArg ->
                        double("z") { zArg ->
                            int("ID") { idArg ->
                                executeSafe {
                                    val entity = EntityDonkey(world)
                                    entity.entityId = getValue(idArg)
                                    val vec = Vec3d(getValue(xArg), getValue(yArg), getValue(zArg))
                                    deployPacket(CPacketUseEntity(entity, getValue(handArg), vec), "${getValue(idArg)} ${getValue(handArg)} $vec")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
