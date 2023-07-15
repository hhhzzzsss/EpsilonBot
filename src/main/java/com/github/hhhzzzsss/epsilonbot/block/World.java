package com.github.hhhzzzsss.epsilonbot.block;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundBlockChangedAckPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

@RequiredArgsConstructor
public class World implements PacketListener, DisconnectListener {
	public final EpsilonBot bot;
	public HashMap<String, CompoundTag> dimensionRegistry = new HashMap<>();
	public HashMap<ChunkPos, ChunkColumn> chunks = new HashMap<>();
	public int sequence = 0;
	public HashMap<Integer, BlockChangeInfo> sequenceMap = new HashMap<>();
	@Getter private int height = 256;
	@Getter private int minY = 0;
	@Getter private String worldName = "";

	// https://github.com/HHH-Kaboom-Dev/HBot/blob/main/src/main/java/com/github/hhhzzzsss/hbot/block/World.java#L108
	private void readDimensionRegistry(CompoundTag registryCodec) {
		ListTag dimensionList = registryCodec.<CompoundTag>get("minecraft:dimension_type").<ListTag>get("value");
		for (Tag tag : dimensionList) {
			CompoundTag dimension = (CompoundTag) tag;
			String name = dimension.<StringTag>get("name").getValue();
			CompoundTag element = dimension.<CompoundTag>get("element");
			dimensionRegistry.put(name, element);
		}
	}
	
	@Override
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundLoginPacket) {
			ClientboundLoginPacket t_packet = (ClientboundLoginPacket) packet;
			readDimensionRegistry(t_packet.getRegistry());
			CompoundTag dimensionEntry = dimensionRegistry.get(t_packet.getDimension());
			height = ((IntTag)dimensionEntry.get("height")).getValue();
			minY = ((IntTag)dimensionEntry.get("min_y")).getValue();
			worldName = t_packet.getWorldName();
		} else if (packet instanceof ClientboundRespawnPacket) {
			ClientboundRespawnPacket t_packet = (ClientboundRespawnPacket) packet;
			CompoundTag dimensionEntry = dimensionRegistry.get(t_packet.getDimension());
			height = ((IntTag)dimensionEntry.get("height")).getValue();
			minY = ((IntTag)dimensionEntry.get("min_y")).getValue();
			if (!worldName.equals(t_packet.getWorldName())) {
				chunks.clear();
				worldName = t_packet.getWorldName();
			}
		}
		if (packet instanceof ClientboundLevelChunkWithLightPacket) {
			ClientboundLevelChunkWithLightPacket t_packet = (ClientboundLevelChunkWithLightPacket) packet;
			ChunkPos pos = new ChunkPos(t_packet.getX(), t_packet.getZ());
			ChunkColumn column;
			try {
				column = new ChunkColumn(pos, t_packet.getChunkData(), height, minY);
			} catch (IOException e) {
				return;
			}
			chunks.put(pos, column);
		} else if (packet instanceof ClientboundBlockUpdatePacket) {
			ClientboundBlockUpdatePacket t_packet = (ClientboundBlockUpdatePacket) packet;
			Vector3i pos = t_packet.getEntry().getPosition();
			int id = t_packet.getEntry().getBlock();
			setBlock(pos.getX(), pos.getY(), pos.getZ(), id);
		} else if (packet instanceof ClientboundSectionBlocksUpdatePacket) {
			ClientboundSectionBlocksUpdatePacket t_packet = (ClientboundSectionBlocksUpdatePacket) packet;
			for (BlockChangeEntry bcr : t_packet.getEntries()) {
				Vector3i pos = bcr.getPosition();
				int id = bcr.getBlock();
				setBlock(pos.getX(), pos.getY(), pos.getZ(), id);
			}
		} else if (packet instanceof ClientboundBlockChangedAckPacket t_packet) {
			final BlockChangeInfo info = this.sequenceMap.remove(t_packet.getSequence());
			if (info == null) return;

			final Vector3i pos = info.getPosition();
			setBlock(pos.getX(), pos.getY(), pos.getZ(), info.getNewId());
		} else if (packet instanceof ClientboundForgetLevelChunkPacket) {
			ClientboundForgetLevelChunkPacket t_packet = (ClientboundForgetLevelChunkPacket) packet;
			chunks.remove(new ChunkPos(t_packet.getX(), t_packet.getZ()));
		}
	}
	
	@Override
	public void onDisconnected(DisconnectedEvent event) {
		chunks.clear();
	}

	public ChunkColumn getChunk(int x, int z) {
		return chunks.get(new ChunkPos(x, z));
	}
	
	public ChunkColumn getChunk(ChunkPos pos) {
		return chunks.get(pos);
	}
	
	public Collection<ChunkColumn> getChunks() {
		return chunks.values();
	}
	
	public int getBlock(int x, int y, int z) {
		ChunkPos chunkPos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
		ChunkColumn chunk = chunks.get(chunkPos);
		return chunk == null ? 0 : chunks.get(chunkPos).getBlock(x&15, y, z&15);
	}
	
	public void setBlock(int x, int y, int z, int id) {
		ChunkPos chunkPos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
		if (!chunks.containsKey(chunkPos)) {
			System.err.println("Tried to set block in nonexistent chunk! This should not happen.");
			return;
		}
		chunks.get(chunkPos).setBlock(x&15, y, z&15, id);
	}

	public void placeBlock(int x, int y, int z, int expectedBlockState) {
		Vector3i vec = Vector3i.from(x, y, z);
		int sequence = this.sequence++;
		bot.sendPacket(new ServerboundUseItemOnPacket(vec, Direction.UP, Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false, sequence));
//		setBlock(x, y, z, expectedBlockState);

		this.sequenceMap.put(sequence, new BlockChangeInfo(vec, expectedBlockState));
	}

	public void breakBlock(int x, int y, int z) {
		Vector3i vec = Vector3i.from(x, y, z);
		int sequence = this.sequence++;
		bot.sendPacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, vec, Direction.UP, sequence));
//		setBlock(x, y, z, 0);

		this.sequenceMap.put(sequence, new BlockChangeInfo(vec, 0));
	}

	public boolean isLoaded(int chunkX, int chunkZ) {
		return chunks.containsKey(new ChunkPos(chunkX, chunkZ));
	}

	public boolean inLoadedChunk(int x, int z) {
		return chunks.containsKey(new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
	}
}
