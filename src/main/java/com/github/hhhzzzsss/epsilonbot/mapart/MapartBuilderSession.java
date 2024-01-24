package com.github.hhhzzzsss.epsilonbot.mapart;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.build.BuilderSession;
import com.github.hhhzzzsss.epsilonbot.build.action.*;
import com.github.hhhzzzsss.epsilonbot.util.BlockUtils;
import com.github.hhhzzzsss.epsilonbot.util.ItemUtils;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MapartBuilderSession extends BuilderSession {
    @Getter URL url;
    boolean useTransparency;
    @Getter String requester;
    @Getter int mapIdx;
    int originX;
    int originZ;
    int numTiles;
    @Getter MapartLoaderThread loaderThread;
    boolean mapartLoaded = false;
    BlockElevation[][] blocks;
    int maxElevation;

    int tileIndex = 0;
    boolean tileLoaded = false;

    int tileProgress = 0;

    public MapartBuilderSession(EpsilonBot bot, MapartBuildState state) {
        super(bot);
        this.url = state.url;
        this.useTransparency = state.useTransparency;
        this.requester = state.requester;
        this.mapIdx = state.mapIdx;
        this.originX = state.originX;
        this.originZ = state.originZ;
        this.blocks = state.blocks;
        this.maxElevation = state.maxElevation;
        this.numTiles = state.numTiles;
        this.tileIndex = state.tileIndex;

        if (blocks == null) {
            bot.sendChat("Something went wrong when recovering mapart state");
            try {
                MapartBuildState.deleteBuildState();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stop();
            return;
        }

        mapartLoaded = true;
        bot.sendPacket(new ServerboundSetCarriedItemPacket(0));
        actionQueue.add(new CommandAction(
                "//limit -1",
                false));
    }

    public MapartBuilderSession(EpsilonBot bot, int mapIdx, URL url, int horizDim, int vertDim, boolean dither, boolean useTransparency, String requester) throws IOException {
        super(bot);
        this.url = url;
        this.useTransparency = useTransparency;
        this.requester = requester;
        this.mapIdx = mapIdx;
        this.originX = Math.floorDiv(Config.getConfig().getMapartX()+64, 128)*128-64;
        this.originZ = Math.floorDiv(Config.getConfig().getMapartZ()+64, 128)*128-64 + 256*mapIdx - 1;
        this.numTiles = horizDim*vertDim;
        this.loaderThread = new MapartLoaderThread(url, horizDim, vertDim, dither, useTransparency);
        this.loaderThread.start();
        bot.sendPacket(new ServerboundSetCarriedItemPacket(0));
        actionQueue.add(new CommandAction(
                "//limit -1",
                false));

        String warpName = Config.getConfig().getWarpName();
        if (!warpName.equals("")) {
            actionQueue.add(new CommandAction(
                    String.format("/setwarp %s_%d", warpName, mapIdx),
                    false));
        }

        actionQueue.add(new WaitAction(3));
    }

    @Override
    public void onAction() {
        if (loaderThread != null) {
            if (loaderThread.isAlive()) {
                return;
            } else if (!mapartLoaded) {
                if (loaderThread.getException() != null) {
                    bot.sendChat("Exception occured while loading mapart: " + loaderThread.getException().getMessage());
                    stop();
                    return;
                }
                blocks = loaderThread.getBlocks();
                maxElevation = loaderThread.getMaxElevation();
                bot.sendChat("Successfully loaded mapart. Starting build...");
                bot.sendPacket(new ServerboundSetCarriedItemPacket(0));
                mapartLoaded = true;
                saveCurrentBuildState();
            }
        }

        // Teleport back if left plot
        if (!isInCorrectTile()) {
            tryTeleport(originX + 128*(tileIndex%numTiles) + 64, 256, originZ + 65, 5);
            return;
        }

        // Check for chunk loading
        if (!allChunksLoaded()) {
            return;
        }

        if (!tileLoaded) {
            if (bot.getPosManager().getY() < 256) {
                tryTeleport(originX + 128*(tileIndex%numTiles) + 64, 256, originZ + 65, 10);
                return;
            }
            try {
                loadTile();
            } catch (Throwable e) {
                e.printStackTrace();
                try {
                    MapartBuildState.deleteBuildState();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                bot.sendChat("An unexpected error occured while building mapart. Stopping...");
                stop();
                return;
            }
            tileLoaded = true;
            saveCurrentBuildState();
        }

        if (!actionQueue.isEmpty()) {
            processAction();
        } else {
            tileIndex++;
            tileLoaded = false;
            tileProgress = 0;
            if (tileIndex >= numTiles*2) {
                try {
                    MapartBuildState.deleteBuildState();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                bot.getWorld().sequence = 0;
                bot.getWorld().sequenceMap.clear();

                String warpName = Config.getConfig().getWarpName();
                if (!warpName.equals("")) {
                    if (requester == null) {
                        bot.sendChat(String.format("Finished building mapart. Go to /warp %s_%d to collect", warpName, mapIdx));
                    } else {
                        bot.sendCommand(String.format("/mail send %s Finished building your mapart. Go to /warp %s_%d to collect", requester, warpName, mapIdx));
                    }
                }

                stop();
                return;
            }
        }
    }



    @Override
    protected void processPlace(PlaceAction action) {
        super.processPlace(action);
        if (tileIndex < numTiles && action.stateId != 0) {
            tileProgress++;
        }
    }

    private boolean isInCorrectTile() {
        int minX = originX + 128*(tileIndex % numTiles);
        int minZ = originZ;
        int maxX = minX+128;
        int maxZ = minZ+129;
        minX -= 5;
        minZ -= 5;
        maxX += 5;
        maxZ += 5;
        if (bot.getPosManager().getX() < minX || bot.getPosManager().getX() > maxX || bot.getPosManager().getZ() < minZ || bot.getPosManager().getZ() > maxZ) {
            return false;
        }
        return true;
    }

    private boolean allChunksLoaded() {
        int chunkXMin = Math.floorDiv(originX + 128*(tileIndex % numTiles), 16);
        int chunkXMax = Math.floorDiv(originX + 128*(tileIndex % numTiles) + 127, 16);
        int chunkZMin = Math.floorDiv(originZ, 16);
        int chunkZMax = Math.floorDiv(originZ + 128, 16);
        for (int chunkX = chunkXMin; chunkX <= chunkXMax; chunkX++) {
            for (int chunkZ = chunkZMin; chunkZ <= chunkZMax; chunkZ++) {
                if (!bot.getWorld().isLoaded(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void loadTile() {
        if (tileIndex < numTiles) {
            loadTileBuild(tileIndex);
        } else {
            loadTileRepair(tileIndex - numTiles);
        }
    }

    @RequiredArgsConstructor
    class BlockChangeEntry {
        public final int x;
        public final int y;
        public final int z;
        public final String name;
    }
    private void loadTileBuild(int idx) {
        boolean topEmpty = true;
        for (int y=100; y<=100+maxElevation; y++) {
            for (int x=128*idx; x<128*idx+127; x++) {
                for (int z = 0; z < 129; z++) {
                    if (bot.getWorld().getBlock(originX+x, y, originZ+z) != 0) {
                        topEmpty = false;
                    }
                }
            }
        }
        if (!topEmpty) {
            actionQueue.add(new CommandAction(
                    String.format("//pos1 %d,%d,%d", originX + 128*idx, 100, originZ),
                    false));
            actionQueue.add(new CommandAction(
                    String.format("//pos2 %d,%d,%d", originX + 128*idx + 127, 100 + maxElevation, originZ+128),
                    false));
            actionQueue.add(new CommandAction(
                    "//set air",
                    true));
        }

        if (useTransparency) {
            int highestGroundBlock = -1;
            for (int y = 0; y < 100; y++) {
                for (int x = 128 * idx; x < 128 * idx + 127; x++) {
                    for (int z = 0; z < 129; z++) {
                        if (bot.getWorld().getBlock(originX + x, y, originZ + z) != 0) {
                            highestGroundBlock = y;
                        }
                    }
                }
            }
            if (highestGroundBlock >= 0) {
                actionQueue.add(new CommandAction(
                        String.format("//pos1 %d,%d,%d", originX + 128 * idx, 0, originZ),
                        false));
                actionQueue.add(new CommandAction(
                        String.format("//pos2 %d,%d,%d", originX + 128 * idx + 127, highestGroundBlock, originZ + 128),
                        false));
                actionQueue.add(new CommandAction(
                        "//set air",
                        true));
            }
        }

        for (int x=128*idx; x<128*idx+128; x++) {
            int direction = x%2==0 ? 1 : -1;
            for (int z = direction==1 ? 0 : 128; z < 129 && z >= 0; z += 8*direction) {
                ArrayList<BlockChangeEntry> blockChangeEntries = new ArrayList<>();
                for (int i=z; i<z+8 && i<129; i++) {
                    BlockElevation be = blocks[x][i];
                    blockChangeEntries.add(new BlockChangeEntry(originX + x, 100+be.elevation, originZ + i, be.block));
                }
                // 0.5 offset for all coords to get block center
                double avgX = blockChangeEntries.stream().mapToDouble(bce -> bce.x).average().getAsDouble() + 0.5;
                double avgY = blockChangeEntries.stream().mapToDouble(bce -> bce.y).average().getAsDouble() + 0.5;
                double avgZ = blockChangeEntries.stream().mapToDouble(bce -> bce.z).average().getAsDouble() + 0.5;
                // position self off to the side to avoid intersecting blocks being placed and 1.5 down because it's based on eye level
                actionQueue.add(new MoveAction(avgX+1, avgY-1.5, avgZ, true, false));
                Map<String, List<BlockChangeEntry>> processedEntries = blockChangeEntries
                        .stream()
                        .filter(bce -> ItemUtils.getItemByName(bce.name) != null)
                        .sorted(Comparator.comparing(bce -> bce.name))
                        .collect(Collectors.groupingBy(bce -> bce.name));
                for (Map.Entry<String, List<BlockChangeEntry>> entry : processedEntries.entrySet()) {
                    int itemId = ItemUtils.getItemByName(entry.getKey()).getId();
                    actionQueue.add(new HoldAction(itemId));
                    int blockState = BlockUtils.getBlockByName(entry.getKey()).getDefaultState();
                    for (BlockChangeEntry bce : entry.getValue()) {
                        actionQueue.add(new PlaceAction(bce.x, bce.y, bce.z, blockState));
                    }
                }
            }
        }
    }

    private void loadTileRepair(int idx) {
        for (int x=128*idx; x<128*idx+128; x++) {
            for (int z = 0; z < 129; z++) {
                BlockElevation be = blocks[x][z];
                int bx = originX + x;
                int by = 100 + be.elevation;
                int bz = originZ + z;
                boolean obscured = false;
                for (int y = 100+be.elevation+1; y <= 100+maxElevation; y++) {
                    if (bot.getWorld().getBlock(bx, y, bz) != 0) {
                        obscured = true;
                    }
                }
                String blockName = BlockUtils.getBlockByStateId(bot.getWorld().getBlock(bx, by, bz)).getName();
                String targetBlockName = be.block;
                if (ItemUtils.getItemByName(targetBlockName) == null) {
                    System.out.println("Failed to get block as item: %s" + targetBlockName);
                    continue;
                }
                if (!blockName.equalsIgnoreCase(targetBlockName) || obscured) {
                    int itemId = ItemUtils.getItemByName(targetBlockName).getId();
                    int blockState = BlockUtils.getBlockByName(targetBlockName).getDefaultState();
                    actionQueue.add(new MoveAction(bx+0.5, 100 + maxElevation+1, bz+0.5, false, true));
                    actionQueue.add(new MoveAction(bx+0.5, by+1, bz+0.5, true, true));
                    actionQueue.add(new HoldAction(itemId));
                    if (!blockName.equals("air")) actionQueue.add(new PlaceAction(bx, by, bz, 0));
                    actionQueue.add(new PlaceAction(bx, by, bz, blockState));
                }
            }
        }
    }

    public void saveCurrentBuildState() {
        MapartBuildState state = new MapartBuildState();
        state.url = this.url;
        state.useTransparency = this.useTransparency;
        state.requester = requester;
        state.mapIdx = this.mapIdx;
        state.originX = this.originX;
        state.originZ = this.originZ;
        state.blocks = this.blocks;
        state.maxElevation = this.maxElevation;
        state.numTiles = this.numTiles;
        state.tileIndex = this.tileIndex;
        try {
            MapartBuildState.saveBuildState(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendStatusMessage(Consumer<? super String> sendFunc) {
        sendFunc.accept("Currently building mapart for: " + (requester==null ? url.toString() : String.format("[Requested by %s]", requester)));
        if (tileIndex < numTiles) {
            int totalBlocks = 128*129*numTiles;
            int totalProgress = 128*129*tileIndex + tileProgress;
            sendFunc.accept(String.format(
                    "%d/%d blocks placed (%.2f%%)",
                    totalProgress,
                    totalBlocks,
                    (double) totalProgress / totalBlocks * 100.0
            ));
        } else {
            sendFunc.accept(String.format(
                    "Checking tiles for errors: %d/%d.",
                    tileIndex-numTiles+1,
                    numTiles
            ));
        }
    }
}
