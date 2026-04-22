package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.DefaultScope
import dev.wizard.meta.util.world.isAir
import dev.wizard.meta.util.world.getMiningSide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.init.Blocks
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraftforge.event.world.NoteBlockEvent
import java.io.*
import java.util.*
import javax.sound.midi.MidiSystem
import javax.sound.midi.ShortMessage
import kotlin.math.log2
import kotlin.math.roundToInt

object NoteBot : Module(
    name = "NoteBot",
    category = Category.MISC,
    description = "Plays music with note blocks"
) {
    private val isNotNbsFormat = { !isNbsFormat() }
    
    private val togglePlay = setting("Toggle Play", false)
    private val reloadSong = setting("Reload Song", false)
    private val songName = setting("Song Name", "Unchanged")
    
    private val channel1 = setting("Channel 1", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel2 = setting("Channel 2", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel3 = setting("Channel 3", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel4 = setting("Channel 4", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel5 = setting("Channel 5", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel6 = setting("Channel 6", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel7 = setting("Channel 7", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel8 = setting("Channel 8", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel9 = setting("Channel 9", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel10 = setting("Channel 10", NoteBlockEvent.Instrument.PIANO) { false } // LambdaUtilsKt.getBOOLEAN_SUPPLIER_FALSE()
    private val channel11 = setting("Channel 11", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel12 = setting("Channel 12", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel13 = setting("Channel 13", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel14 = setting("Channel 14", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel15 = setting("Channel 15", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }
    private val channel16 = setting("Channel 16", NoteBlockEvent.Instrument.PIANO) { isNotNbsFormat() }

    private var startTime = 0L
    private var elapsed = 0L
    private var duration = 0L
    private var playingSong = false
    private var noteSequence = TreeMap<Long, ArrayList<Note>>()
    private val noteBlockMap = EnumMap<NoteBlockEvent.Instrument, Array<BlockPos?>>(NoteBlockEvent.Instrument::class.java)
    private val noteBlocks = ArrayList<BlockPos>()
    private val clickedBlocks = HashSet<BlockPos>()
    private val soundTimer = TickTimer(TimeUnit.SECONDS)
    private val channelSettings: Array<EnumSetting<NoteBlockEvent.Instrument>> = arrayOf(
        channel1, channel2, channel3, channel4, channel5, channel6, channel7, channel8, 
        channel9, channel10, channel11, channel12, channel13, channel14, channel15, channel16
    )

    override fun getHudInfo(): String {
        return if (playingSong) "Playing" else "Stopped"
    }

    init {
        onEnable {
            if (mc.player?.isCreative == true) {
                NoSpamMessage.sendMessage(this, "You are in creative mode and cannot play music.")
                disable()
            } else {
                loadSong()
                scanNoteBlocks()
            }
        }

        ListenerKt.listener<TickEvent.Post> {
            if (noteBlocks.isNotEmpty()) {
                val pos = noteBlocks.removeAt(noteBlocks.lastIndex)
                clickBlock(pos)
                clickedBlocks.add(pos)
            } else if (soundTimer.tick(5L)) {
                noteBlocks.addAll(clickedBlocks)
                clickedBlocks.clear()
            }
        }

        ListenerKt.listener<PacketEvent.Receive> {
            if (it.packet is SPacketSoundEffect) {
                val packet = it.packet as SPacketSoundEffect
                if (noteBlocks.isNotEmpty() && clickedBlocks.isNotEmpty() && packet.category == SoundCategory.RECORDS) {
                    val soundEvent = packet.sound
                    val instrument = getInstrument(soundEvent)
                    if (instrument != null) {
                        val pos = BlockPos(packet.x, packet.y, packet.z)
                        if (clickedBlocks.remove(pos)) {
                            val pitch = (log2(packet.pitch.toDouble()) * 12.0).roundToInt() + 12
                            val array = noteBlockMap.computeIfAbsent(instrument) { arrayOfNulls(25) }
                            array[pitch.coerceIn(0, 24)] = pos
                            soundTimer.reset()
                        }
                    }
                }
            }
        }

        ListenerKt.listener<Render3DEvent> {
            if (noteBlocks.isEmpty() && clickedBlocks.isEmpty()) {
                if (playingSong) {
                    if (mc.player?.isCreative == false) {
                        while (noteSequence.isNotEmpty()) {
                            val time = noteSequence.firstKey()
                            if (time > elapsed) break
                            val notes = noteSequence.pollFirstEntry().value
                            playNotes(notes)
                        }
                        if (noteSequence.isEmpty()) {
                            NoSpamMessage.sendMessage(this, "Finished playing song.")
                            setPlayingSong(false)
                        }
                        elapsed = System.currentTimeMillis() - startTime
                    } else {
                        setPlayingSong(false)
                        NoSpamMessage.sendMessage(this, "You are in creative mode and cannot play music.")
                    }
                }
            }
        }

        togglePlay.listeners.add {
            if (togglePlay.value) {
                if (isEnabled) {
                    setPlayingSong(!playingSong)
                    NoSpamMessage.sendMessage(this, if (playingSong) "Start playing!" else "Pause playing!")
                }
                togglePlay.value = false
            }
        }

        reloadSong.listeners.add {
            if (reloadSong.value) {
                if (isEnabled) {
                    loadSong()
                }
                reloadSong.value = false
            }
        }
    }

    private fun isNbsFormat(): Boolean {
        return songName.value.endsWith(".nbs", ignoreCase = true)
    }

    private fun setPlayingSong(value: Boolean) {
        startTime = System.currentTimeMillis() - elapsed
        playingSong = value
    }

    private fun loadSong() {
        DefaultScope.launch(Dispatchers.IO) {
            val path = "trollhack/songs/" + songName.value
            try {
                val it = parse(path)
                noteSequence = it
                duration = it.lastKey()
                NoSpamMessage.sendMessage(this@NoteBot, "Loaded song $path")
            } catch (e: Exception) {
                NoSpamMessage.sendMessage(this@NoteBot, "Error loading song $path: ${e.message}")
                disable()
            }
            duration = 0L
            elapsed = 0L
            setPlayingSong(false)
        }
    }

    private fun parse(fileName: String): TreeMap<Long, ArrayList<Note>> {
        return if (isNbsFormat()) {
            sortOutInstruments()
            readNbs(fileName)
        } else {
            readMidi(fileName)
        }
    }

    private fun sortOutInstruments() {
        channel1.value = NoteBlockEvent.Instrument.PIANO
        channel2.value = NoteBlockEvent.Instrument.BASSGUITAR
        channel3.value = NoteBlockEvent.Instrument.BASSDRUM
        channel4.value = NoteBlockEvent.Instrument.SNARE
        channel5.value = NoteBlockEvent.Instrument.CLICKS
        channel6.value = NoteBlockEvent.Instrument.GUITAR
        channel7.value = NoteBlockEvent.Instrument.FLUTE
        channel8.value = NoteBlockEvent.Instrument.BELL
        channel9.value = NoteBlockEvent.Instrument.CHIME
        channel10.value = NoteBlockEvent.Instrument.XYLOPHONE
    }

    private fun readNbs(fileName: String): TreeMap<Long, ArrayList<Note>> {
        val noteSequence = TreeMap<Long, ArrayList<Note>>()
        val file = File(fileName)
        val inputStream = FileInputStream(file)
        val dataInputStream = DataInputStream(BufferedInputStream(inputStream))
        
        val length = dataInputStream.readShort()
        var nbsVersion: Byte = 0
        if (length.toInt() == 0) {
            nbsVersion = dataInputStream.readByte()
            dataInputStream.readByte() // index
            if (nbsVersion >= 3) {
                readShortCustom(dataInputStream) // song length
            }
        }
        readShortCustom(dataInputStream) // Layer height
        skipString(dataInputStream) // Song name
        skipString(dataInputStream) // Song author
        skipString(dataInputStream) // Song original author
        skipString(dataInputStream) // Song description
        val tempo = readShortCustom(dataInputStream)
        val timeBetween = 1000L / (tempo / 100)
        dataInputStream.skipBytes(23)
        skipString(dataInputStream) // loop name
        if (nbsVersion >= 4) {
            dataInputStream.skipBytes(4) // loop start/max loop count
        }

        var currentTick = -1
        var jump: Short
        while (readShortCustom(dataInputStream).also { jump = it }.toInt() != 0) {
            currentTick += jump
            var layer = -1
            var jumpLayer: Short
            while (readShortCustom(dataInputStream).also { jumpLayer = it }.toInt() != 0) {
                layer += jumpLayer
                val instrument = dataInputStream.readByte()
                val key = dataInputStream.readByte()
                if (nbsVersion >= 4) {
                    dataInputStream.readByte() // velocity
                    dataInputStream.readByte() // panning
                    readShortCustom(dataInputStream) // pitch
                }
                val time = timeBetween * currentTick
                val note = key % 36
                noteSequence.computeIfAbsent(time) { ArrayList() }.add(Note(note, instrument.toInt().coerceIn(0, 15)))
            }
        }
        return noteSequence
    }

    private fun readShortCustom(dataInputStream: DataInputStream): Short {
        val byte1 = dataInputStream.readUnsignedByte()
        val byte2 = dataInputStream.readUnsignedByte()
        return (byte1 or (byte2 shl 8)).toShort()
    }

    private fun readIntCustom(dataInputStream: DataInputStream): Int {
        val byte1 = dataInputStream.readUnsignedByte()
        val byte2 = dataInputStream.readUnsignedByte()
        val byte3 = dataInputStream.readUnsignedByte()
        val byte4 = dataInputStream.readUnsignedByte()
        return byte1 or (byte2 shl 8) or (byte3 shl 16) or (byte4 shl 24)
    }

    private fun skipString(dataInputStream: DataInputStream) {
        dataInputStream.skip(readIntCustom(dataInputStream).toLong())
    }

    private fun readMidi(fileName: String): TreeMap<Long, ArrayList<Note>> {
        val sequence = MidiSystem.getSequence(File(fileName))
        val noteSequence = TreeMap<Long, ArrayList<Note>>()
        val resolution = sequence.resolution
        
        for (track in sequence.tracks) {
            for (i in 0 until track.size()) {
                val event = track.get(i)
                val midiMessage = event.message
                if (midiMessage is ShortMessage && midiMessage.command == ShortMessage.NOTE_ON) {
                    val tick = event.tick
                    val time = (tick * (500000.0 / resolution) / 1000.0 + 0.5).toLong()
                    val note = midiMessage.data1 % 36
                    val channel = midiMessage.channel
                    noteSequence.computeIfAbsent(time) { ArrayList() }.add(Note(note, channel.coerceIn(0, 15)))
                }
            }
        }
        return noteSequence
    }

    private fun scanNoteBlocks() {
        val player = mc.player ?: return
        for (x in -5..5) {
            for (y in -3..6) {
                for (z in -5..5) {
                    val pos = player.position.add(x, y, z)
                    if (!mc.world.isAir(pos.x, pos.y + 1, pos.z) || mc.world.getBlockState(pos).block != Blocks.NOTEBLOCK) continue
                    noteBlocks.add(pos)
                }
            }
        }
    }

    private fun getInstrument(soundEvent: SoundEvent): NoteBlockEvent.Instrument? {
        return when (soundEvent) {
            SoundEvents.BLOCK_NOTE_HARP -> NoteBlockEvent.Instrument.PIANO
            SoundEvents.BLOCK_NOTE_BASEDRUM -> NoteBlockEvent.Instrument.BASSDRUM
            SoundEvents.BLOCK_NOTE_SNARE -> NoteBlockEvent.Instrument.SNARE
            SoundEvents.BLOCK_NOTE_HAT -> NoteBlockEvent.Instrument.CLICKS
            SoundEvents.BLOCK_NOTE_BASS -> NoteBlockEvent.Instrument.BASSGUITAR
            SoundEvents.BLOCK_NOTE_FLUTE -> NoteBlockEvent.Instrument.FLUTE
            SoundEvents.BLOCK_NOTE_BELL -> NoteBlockEvent.Instrument.BELL
            SoundEvents.BLOCK_NOTE_GUITAR -> NoteBlockEvent.Instrument.GUITAR
            SoundEvents.BLOCK_NOTE_CHIME -> NoteBlockEvent.Instrument.CHIME
            SoundEvents.BLOCK_NOTE_XYLOPHONE -> NoteBlockEvent.Instrument.XYLOPHONE
            else -> null
        }
    }

    private fun playNotes(notes: List<Note>) {
        for (note in notes) {
            if (note.track == 9 && !isNbsFormat()) {
                val instrument = getPercussionInstrument(note.note) ?: continue
                val blockPosArray = noteBlockMap[instrument]
                val pos = blockPosArray?.firstOrNull { it != null }
                if (pos != null) clickBlock(pos)
                continue
            }
            val instrument = channelSettings[note.track].value
            val pitch = note.getNoteBlockNote()
            val blockPosArray = noteBlockMap[instrument]
            val pos = blockPosArray?.getOrNull(pitch)
            if (pos != null) clickBlock(pos)
        }
    }

    private fun getPercussionInstrument(note: Int): NoteBlockEvent.Instrument? {
        return when (note) {
            0 -> NoteBlockEvent.Instrument.BASSDRUM
            2, 4 -> NoteBlockEvent.Instrument.SNARE
            1, 6, 8, 10 -> NoteBlockEvent.Instrument.CLICKS
            else -> null
        }
    }

    private fun clickBlock(pos: BlockPos) {
        val side = getMiningSide(pos) ?: EnumFacing.UP
        mc.connection?.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side))
        mc.connection?.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, side))
        mc.player.swingArm(EnumHand.MAIN_HAND)
    }

    private data class Note(val note: Int, val track: Int) {
        fun getNoteBlockNote(): Int {
            var key = (note - 6) % 24
            if (key < 0) key += 24
            return key
        }
    }
}