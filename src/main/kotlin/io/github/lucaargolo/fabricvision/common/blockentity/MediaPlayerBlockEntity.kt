package io.github.lucaargolo.fabricvision.common.blockentity

import io.github.lucaargolo.fabricvision.player.MinecraftMediaPlayer
import io.github.lucaargolo.fabricvision.player.MinecraftMediaPlayerHolder
import io.github.lucaargolo.fabricvision.utils.FramebufferTexture
import io.github.lucaargolo.fabricvision.utils.ModIdentifier
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.render.Camera
import net.minecraft.command.argument.EntityAnchorArgumentType
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.*

abstract class MediaPlayerBlockEntity(type: BlockEntityType<out MediaPlayerBlockEntity>, pos: BlockPos, state: BlockState) : BlockEntity(type, pos, state) {

    private val internalPlayer: MinecraftMediaPlayer by lazy {
        MinecraftMediaPlayerHolder.create(uuid!!)
    }
    val player: MinecraftMediaPlayer?
        get() = if(uuid != null) internalPlayer else null

    var uuid: UUID? = null

    var mrl = "https://cdn.discordapp.com/attachments/253728532939669504/1131109086407163924/SaveTube.io-Rick_Astley_-_Never_Gonna_Give_You_Up_Official_Music_Video.mp4"
        set(value) {
            field = value
            markDirtyAndSync()
        }

    var time = 0L
        set(value) {
            field = value
            markDirtyAndSync()
        }

    var playing = true
        set(value) {
            field = value
            markDirtyAndSync()
        }

    override fun writeNbt(nbt: NbtCompound) {
        if(uuid != null) {
            nbt.putUuid("uuid", uuid)
        }
        nbt.putString("mrl", mrl)
        nbt.putLong("time", time)
        nbt.putBoolean("playing", playing)
    }

    override fun readNbt(nbt: NbtCompound) {
        uuid = if(nbt.contains("uuid")) {
            nbt.getUuid("uuid")
        }else{
            UUID.randomUUID()
        }
        mrl = nbt.getString("mrl")
        time = nbt.getLong("time")
        playing = nbt.getBoolean("playing")
    }

    override fun toInitialChunkDataNbt(): NbtCompound {
        return NbtCompound().also(::writeNbt)
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? {
        return BlockEntityUpdateS2CPacket.create(this) {
            toInitialChunkDataNbt()
        }
    }

    fun markDirtyAndSync() {
        markDirty()
        sync()
    }
    fun sync() {
        (world as? ServerWorld)?.chunkManager?.markForUpdate(this.pos)
    }

    override fun markRemoved() {
        super.markRemoved()
        if(world?.isClient == true) {
            player?.close()
        }
    }

    class FlatScreen(pos: BlockPos, state: BlockState): MediaPlayerBlockEntity(BlockEntityCompendium.FLAT_SCREEN, pos, state)
    class Projector(pos: BlockPos, state: BlockState): ProjectorBlockEntity(pos, state)


    companion object {



        fun clientTick(world: World, pos: BlockPos, state: BlockState, blockEntity: MediaPlayerBlockEntity) {
            blockEntity.player?.pos = Vec3d.ofCenter(pos)
            blockEntity.player?.mrl = blockEntity.mrl
            blockEntity.player?.time = blockEntity.time
            blockEntity.player?.playing = blockEntity.playing
        }

    }

}