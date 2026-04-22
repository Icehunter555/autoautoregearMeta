package dev.wizard.meta.event

import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.event.events.*
import dev.wizard.meta.event.events.baritone.BaritoneCommandEvent
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.events.player.InteractEvent
import dev.wizard.meta.event.events.player.PlayerPushOutOfBlockEvent
import dev.wizard.meta.event.events.render.*
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.gui.mc.TrollGuiChat
import dev.wizard.meta.manager.managers.WorldManager
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.text.MessageDetection
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.*
import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL20
import java.util.*

object ForgeEventProcessor : ListenerOwner() {
    private val mc: Minecraft = Wrapper.minecraft
    private var prevWidth = -1
    private var prevHeight = -1

    init {
        listener<TickEvent.Post>(alwaysListening = true) {
            if (prevWidth != mc.displayWidth || prevHeight != mc.displayHeight) {
                prevWidth = mc.displayWidth
                prevHeight = mc.displayHeight
                ResolutionUpdateEvent(mc.displayWidth, mc.displayHeight).post()
                GL20.glUseProgram(0)
            }
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        ProjectionUtils.updateMatrix()
        RenderUtils3D.prepareGL()
        Render3DEvent.post()
        LastRenderWorldEvent(event).post()
        RenderUtils3D.releaseGL()
        GL20.glUseProgram(0)
    }

    @SubscribeEvent
    fun onRenderGameOverlayPre(event: RenderGameOverlayEvent.Pre) {
        GlStateUtils.alpha(false)
        RenderOverlayEvent.Pre(event).post()
        GlStateUtils.alpha(true)
    }

    @SubscribeEvent
    fun onRenderGameOverlayPost(event: RenderGameOverlayEvent.Post) {
        GlStateUtils.alpha(false)
        RenderOverlayEvent.Post(event).post()
        GlStateUtils.alpha(true)
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        val key = Keyboard.getEventKey()
        val state = Keyboard.getEventKeyState()
        InputEvent.Keyboard(key, state).post()
        if (!state) return

        if (!mc.gameSettings.keyBindChat.isKeyDown) {
            val prefix = CommandManager.prefix
            val typedChar = Keyboard.getEventCharacter().toString()
            if (prefix.length == 1 && typedChar.equals(prefix, ignoreCase = true)) {
                mc.displayGuiScreen(TrollGuiChat(prefix))
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChatSent(event: ClientChatEvent) {
        val message = event.message
        MessageDetection.Command.BARITONE.removedOrNull(message)?.let {
            val cmd = it.toString().substringBefore(' ').lowercase(Locale.ROOT)
            BaritoneCommandEvent(cmd).post()
        }
        if (MessageDetection.Command.TROLL_HACK.detect(message)) {
            CommandManager.runCommand(message.removePrefix(CommandManager.prefix))
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onEventMouse(event: InputEvent.MouseInputEvent) {
        InputEvent.Mouse(Mouse.getEventButton(), Mouse.getEventButtonState()).post()
    }

    @SubscribeEvent
    fun onRenderBlockOverlay(event: net.minecraftforge.client.event.RenderBlockOverlayEvent) {
        RenderBlockOverlayEvent(event).post()
    }

    @SubscribeEvent
    fun onInputUpdate(event: net.minecraftforge.client.event.InputUpdateEvent) {
        InputUpdateEvent(event).post()
    }

    @SubscribeEvent
    fun onClientDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        ConnectionEvent.Disconnect.post()
    }

    @SubscribeEvent
    fun onClientConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        ConnectionEvent.Connect.post()
    }

    @SubscribeEvent
    fun onRenderFogColors(event: EntityViewRenderEvent.FogColors) {
        FogColorEvent(event).post()
    }

    @SubscribeEvent
    fun onRenderFogDensity(event: EntityViewRenderEvent.FogDensity) {
        FogDensityEvent(event).post()
    }

    @SubscribeEvent
    fun onRenderCameraSetup(event: EntityViewRenderEvent.CameraSetup) {
        CameraSetupEvent(event).post()
    }

    @SubscribeEvent
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        InteractEvent.Item.RightClick(event.hand).post()
    }

    @SubscribeEvent
    fun onSoundPlayed(event: PlaySoundEvent) {
        SoundPlayedEvent(event).post()
    }

    @SubscribeEvent
    fun onLoadWorld(event: WorldEvent.Load) {
        if (event.world.isRemote) {
            event.world.addEventListener(WorldManager)
            WorldEvent.Load.post()
        }
    }

    @SubscribeEvent
    fun onUnloadWorld(event: WorldEvent.Unload) {
        if (event.world.isRemote) {
            event.world.removeEventListener(WorldManager)
            WorldEvent.Unload.post()
        }
    }

    @SubscribeEvent
    fun onPlayerSPPushOutOfBlocks(event: PlayerSPPushOutOfBlocksEvent) {
        PlayerPushOutOfBlockEvent(event).post()
    }

    @SubscribeEvent
    fun onClientChatReceive(event: ClientChatReceivedEvent) {
        ChatReceiveEvent(event).post()
    }

    @SubscribeEvent
    fun onRenderHand(event: net.minecraftforge.client.event.RenderHandEvent) {
        RenderHandEvent.Pre(event).post()
        if (!event.isCanceled) {
            RenderHandEvent.Post(event).post()
        }
    }

    @SubscribeEvent
    fun onRenderSpecificHand(event: net.minecraftforge.client.event.RenderSpecificHandEvent) {
        RenderSpecifiedHandEvent(event).post()
    }
}
