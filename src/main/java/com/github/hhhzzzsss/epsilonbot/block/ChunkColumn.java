package com.github.hhhzzzsss.epsilonbot.block;

import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class ChunkColumn {
	@Getter private final ChunkPos pos;
	@Getter private ChunkSection[] chunks;
	private int minY;

	public ChunkColumn(ChunkPos chunkPos, byte[] data, int worldHeight, int minY) throws IOException {
		this.pos = chunkPos;
		this.minY = minY;
		ByteBuf byteBuf = Unpooled.wrappedBuffer(data);
		NetInput in = new ByteBufNetInput(byteBuf);
		int numSections = -Math.floorDiv(-worldHeight, 16);
		chunks = new ChunkSection[numSections];
		for (int i=0; i<numSections; i++) {
			chunks[i] = ChunkSection.read(in, 15);
		}
	}
	
	public int getBlock(int x, int y, int z) {
		int yIdx = (y-minY)>>4;
		if (chunks[yIdx] == null) return 0;
		return chunks[yIdx].getBlock(x, y&15, z);
	}
	
	public void setBlock(int x, int y, int z, int id) {
		int yIdx = (y-minY)>>4;
		if (chunks[yIdx] == null) {
			chunks[yIdx] = new ChunkSection();
			chunks[yIdx].setBlock(0, 0, 0, 0);
		}
		chunks[yIdx].setBlock(x, y&15, z, id);
	}
}
