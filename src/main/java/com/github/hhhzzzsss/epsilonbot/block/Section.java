package com.github.hhhzzzsss.epsilonbot.block;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class Section {
	@Getter private final int xdim;
	@Getter private final int ydim;
	@Getter private final int zdim;
	@Getter private int[] blocks;
	@Getter private ArrayList<String> idMap = new ArrayList<>();
	
	public Section(int xdim, int ydim, int zdim) {
		this.xdim = xdim;
		this.ydim = ydim;
		this.zdim = zdim;
		this.blocks = new int[xdim*ydim*zdim];
	}

	public static Section loadFromSchem(InputStream in) throws IOException {
		Tag tag = NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(in)));
		if(!(tag instanceof CompoundTag)) {
			throw new IOException("Not a valid schematic");
		}
		CompoundTag nbt = (CompoundTag) tag;

		ShortTag widthtag = nbt.get("Width");
		ShortTag heighttag = nbt.get("Height");
		ShortTag lengthtag = nbt.get("Length");
		int width = widthtag.getValue();
		int height = heighttag.getValue();
		int length = lengthtag.getValue();
		Section section = new Section(width, height, length);

		CompoundTag palette = nbt.get("Palette");
		ByteArrayTag blockdata = nbt.get("BlockData");

		String[] paletteArr = new String[palette.size()];
		int bpb = 1;
		while (palette.size() >> bpb > 0) {bpb++;}
		for (Tag paletteEntry : palette) {
			IntTag intEntry = (IntTag) paletteEntry;
			paletteArr[intEntry.getValue()] = intEntry.getName();
		}
		section.setPaletteEntries(paletteArr);

		int varInt = 0;
		int varIntLength = 0;
		int storageIdx = 0;
		for (int i = 0; i < blockdata.length(); i++) {
			varInt |= (int)(blockdata.getValue(i) & 127) << (varIntLength++ * 7);
			if ((blockdata.getValue(i) & 128) == 128) {
				continue;
			}

			section.setId(storageIdx++, varInt);

			varInt = 0;
			varIntLength = 0;
		}

		return section;
	}
	
	public String getBlock(int i) {
		return idMap.get(blocks[i]);
	}
	
	public String getBlock(int x, int y, int z) {
		return idMap.get(blocks[index(x, y, z)]);
	}
	
	public void setId(int i, int id) {
		blocks[i] = id;
	}
	
	public void setId(int x, int y, int z, int id) {
		blocks[index(x, y, z)] = id;
	}
	
	public int addPaletteEntry(String block) {
		if (block.startsWith("minecraft:")) block = block.substring(10);
		block = block.replaceAll("\\[.*\\]", "");
		idMap.add(block);
		return idMap.size()-1;
	}
	
	public void setPaletteEntries(String... blocks) {
		idMap.clear();
		for (String block : blocks) {
			addPaletteEntry(block);
		}
	}
	
	public void setPaletteEntries(Iterable<String> blocks) {
		idMap.clear();
		for (String block : blocks) {
			addPaletteEntry(block);
		}
	}
	
	public int index(int x, int y, int z) {
		return y*xdim*zdim + z*xdim + x;
	}
	
	public Vector3i decomposeIndex(int index) {
		int x = index % xdim;
		index /= xdim;
		int z = index % zdim;
		index /= zdim;
		int y = index;
		return Vector3i.from(x, y, z);
	}
	
//	@AllArgsConstructor
//	private class Subdivision {
//		public int x1, y1, z1, x2, y2, z2, id;
//	}
//
//	public ArrayList<Subdivision> subdivisions;
//
//	public void cubicOptimize(int maxSize) {
//		subdivisions = new ArrayList<>();
//		boolean included[] = new boolean[size()];
//		for (int i=0; i<size(); i++) {
//			if (included[i]) continue;
//			if (!hasFlag(SectionFlag.FILLAIR) && getBlock(i).equals("air")) continue;
//
//			int blockId = blocks[i];
//			Vector3i xyz = decomposeIndex(i);
//			Subdivision subdiv = new Subdivision(xyz.getX(), xyz.getY(), xyz.getZ(), xyz.getX(), xyz.getY(), xyz.getZ(), blockId);
//			boolean canExpandX = true;
//			boolean canExpandY = true;
//			boolean canExpandZ = true;
//			while (canExpandX || canExpandY || canExpandZ) {
//				int xExpandedSize = (subdiv.x2-subdiv.x1+2)*(subdiv.y2-subdiv.y1+1)*(subdiv.z2-subdiv.z1+1);
//				int yExpandedSize = (subdiv.x2-subdiv.x1+1)*(subdiv.y2-subdiv.y1+2)*(subdiv.z2-subdiv.z1+1);
//				int zExpandedSize = (subdiv.x2-subdiv.x1+1)*(subdiv.y2-subdiv.y1+2)*(subdiv.z2-subdiv.z1+2);
//				if (xExpandedSize > maxSize || subdiv.x2 == xdim-1) canExpandX = false;
//				if (yExpandedSize > maxSize || subdiv.y2 == ydim-1) canExpandY = false;
//				if (zExpandedSize > maxSize || subdiv.z2 == zdim-1) canExpandZ = false;
//				if (canExpandX) {
//					for (int y=subdiv.y1; y<=subdiv.y2; y++) for (int z=subdiv.z1; z<=subdiv.z2; z++) {
//						int subIdx = index(subdiv.x2+1, y, z);
//						if (blocks[subIdx] != blockId || included[subIdx]) {
//							canExpandX = false;
//							break;
//						}
//					}
//				}
//				if (canExpandY) {
//					for (int x=subdiv.x1; x<=subdiv.x2; x++) for (int z=subdiv.z1; z<=subdiv.z2; z++) {
//						int subIdx = index(x, subdiv.y2+1, z);
//						if (blocks[subIdx] != blockId || included[subIdx]) {
//							canExpandY = false;
//							break;
//						}
//					}
//				}
//				if (canExpandZ) {
//					for (int x=subdiv.x1; x<=subdiv.x2; x++) for (int y=subdiv.y1; y<=subdiv.y2; y++) {
//						int subIdx = index(x, y, subdiv.z2+1);
//						if (blocks[subIdx] != blockId || included[subIdx]) {
//							canExpandZ = false;
//							break;
//						}
//					}
//				}
//
//				int maxExpand = 0;
//				int expandIdx = -1;
//				if (canExpandX && xExpandedSize > maxExpand) {
//					maxExpand = xExpandedSize;
//					expandIdx = 0;
//				}
//				if (canExpandY && yExpandedSize > maxExpand) {
//					maxExpand = yExpandedSize;
//					expandIdx = 1;
//				}
//				if (canExpandZ && zExpandedSize > maxExpand) {
//					maxExpand = zExpandedSize;
//					expandIdx = 2;
//				}
//
//				if (expandIdx == 0) {
//					subdiv.x2++;
//				}
//				else if (expandIdx == 1) {
//					subdiv.y2++;
//				}
//				else if (expandIdx == 2) {
//					subdiv.z2++;
//				}
//			}
//			for (int x=subdiv.x1; x<=subdiv.x2; x++) for (int y=subdiv.y1; y<=subdiv.y2; y++) for (int z=subdiv.z1; z<=subdiv.z2; z++) {
//				included[index(x,y,z)] = true;
//			}
//			subdivisions.add(subdiv);
//		}
//	}
//
//	public void hollow(int interiorId) {
//		boolean[][][] flags = new boolean[xdim][ydim][zdim];
//		for (int x = 1; x < xdim-1; x++) {
//			for (int y = 1; y < ydim-1; y++) {
//				for (int z = 1; z < zdim-1; z++) {
//					if (!getBlock(x,y,z).equals("air") &&
//							!getBlock(x-1,y,z).equals("air") && !getBlock(x+1,y,z).equals("air") &&
//							!getBlock(x,y-1,z).equals("air") && !getBlock(x,y+1,z).equals("air") &&
//							!getBlock(x,y,z-1).equals("air") && !getBlock(x,y,z+1).equals("air")) {
//						flags[x][y][z] = true;
//					}
//				}
//			}
//		}
//		for (int x = 1; x < xdim-1; x++) {
//			for (int y = 1; y < ydim-1; y++) {
//				for (int z = 1; z < zdim; z++) {
//					if (flags[x][y][z]) {
//						setId(x, y, z, interiorId);
//					}
//				}
//			}
//		}
//	}
//
//	@Getter @Setter private int index = 0;
//	public String nextCommand() {
//		if (subdivisions == null) {
//			return nextUnoptimizedCommand();
//		}
//		else {
//			return nextOptimizedCommand();
//		}
//	}
//
//	private String nextUnoptimizedCommand() {
//		if (!hasFlag(SectionFlag.FILLAIR)) {
//			while (index < size() && getBlock(index).equals("air")) {
//				index++;
//			}
//		}
//		if (index < size()) {
//			Vector3i xyz = decomposeIndex(index);
//			xyz = xyz.offset(xorig, yorig, zorig);
//			if (hasFlag(SectionFlag.HCENTERED) || hasFlag(SectionFlag.CENTERED)) {
//				xyz = xyz.offset(-xdim/2, 0, -zdim/2);
//			}
//			if (hasFlag(SectionFlag.VCENTERED) || hasFlag(SectionFlag.CENTERED)) {
//				xyz = xyz.offset(0, -ydim/2, 0);
//			}
//
//			String command = String.format("setblock %d %d %d %s replace", xyz.getX(), xyz.getY(), xyz.getZ(), getBlock(index));
//			index++;
//			return command;
//		}
//		else {
//			return null;
//		}
//	}
}
