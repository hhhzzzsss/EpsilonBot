package com.github.hhhzzzsss.epsilonbot.buildsync;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.block.Section;
import com.github.hhhzzzsss.epsilonbot.block.World;
import com.github.hhhzzzsss.epsilonbot.buildsync.action.*;
import com.github.hhhzzzsss.epsilonbot.modules.PositionManager;
import com.github.hhhzzzsss.epsilonbot.util.BlockUtils;
import com.github.hhhzzzsss.epsilonbot.util.ItemUtils;
import com.github.hhhzzzsss.epsilonbot.util.PlotUtils;
import com.github.hhhzzzsss.epsilonbot.util.Vec3d;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlotBuilderSession {
    public static final String FLOOR_BLOCK = "glass";
    public static final int LAYER_LIMIT = (PlotUtils.PLOT_DIM+1) / 2;
    public static final Pattern WE_PROGRESS_PATTERN = Pattern.compile("\\s*You have \\S+ blocks queued. Placing speed: \\S+, \\S+ left.\\s*");
    public static final Pattern WE_DONE_PATTERN = Pattern.compile("\\s*Job \\[\\S+\\] \\S+ - done\\s*");

    public final EpsilonBot bot;
    public final Section section;
    public final PlotCoord plotCoord;
    public final String plotName;
    public final int originX;
    public final int originZ;
    @Getter private boolean finished = false;
    @Getter private boolean stopped = false;

    int currentLayerProgress = 0;

    Queue<Action> actionQueue = new LinkedList<>();

    boolean firstLoad = false;
    int currentLayer = 0;
    int WECooldown = 0;
    boolean WEFinished = false;
    int waitTime = 0;
    boolean waiting = true;

    public PlotBuilderSession(EpsilonBot bot, Section section, PlotCoord plotCoord, String plotName) {
        this.bot = bot;
        this.section = section;
        this.plotCoord = plotCoord;
        this.plotName = plotName;
        this.originX = plotCoord.getX() * PlotUtils.PLOT_DIM + Config.getConfig().getBuildSyncX();
        this.originZ = plotCoord.getZ() * PlotUtils.PLOT_DIM + Config.getConfig().getBuildSyncZ();
    }

    public void onAction() {
        // Teleport back if left plot
        if (!PlotUtils.isInPlot(bot.getPosManager().getX(), bot.getPosManager().getZ(), plotCoord)) {
            if (teleportCooldown <= 0) {
                sendStartingPositionTpCommand();
                teleportCooldown = 20;
            }
            return;
        }

        // Check for chunk loading
        if (!firstLoad) {
            if (allChunksLoaded() && bot.getPosManager().getY() >= PlotUtils.PLOT_DIM) {
                firstLoad = true;
                actionQueue.add(new WECommandAction(
                        "//limit -1",
                        false));
                currentLayer = getFirstLayerDifference();
                doInitialSets();
            } else if (!isInStartingPosition()) {
                if (teleportCooldown <= 0) {
                    sendStartingPositionTpCommand();
                    teleportCooldown = 20;
                }
                return;
            } else {
                return;
            }
        }

        if (!actionQueue.isEmpty()) {
            processAction();
        } else {
            if (!allChunksLoaded()) {
                actionQueue.add(new MoveAction(originX + PlotUtils.PLOT_DIM/2, currentLayer*2 + 2, originZ + PlotUtils.PLOT_DIM/2));
                return;
            } else {
                while (getLayerDifferences(currentLayer) == 0 && currentLayer < LAYER_LIMIT) {
                    if (currentLayer == 0) {
                        actionQueue.add(new WECommandAction(
                                String.format("//pos1 %d,0,%d", originX, originZ),
                                false));
                        actionQueue.add(new WECommandAction(
                                String.format("//pos2 %d,0,%d", originX+PlotUtils.PLOT_DIM-1, originZ+PlotUtils.PLOT_DIM-1),
                                false));
                        actionQueue.add(new WECommandAction(
                                String.format("//replace air %s", FLOOR_BLOCK),
                                true));
                    }
                    currentLayer++;
                }
                if (currentLayer >= LAYER_LIMIT) {
                    stop(true);
                } else {
                    updateCurrentLayerProgress();
                    loadLayer();
                }
            }
        }
    }

    int teleportCooldown = 0;
    public void onTimePacket() {
        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
        if (WECooldown > 0) {
            WECooldown--;
        }
        if (waiting) {
            waitTime++;
        }
    }

    public void onChat(String message) {
        if (WE_PROGRESS_PATTERN.matcher(message).matches()) {
            WECooldown = 10;
        } else if (WE_DONE_PATTERN.matcher(message).matches()) {
            WEFinished = true;
        }
    }

    public boolean allChunksLoaded() {
        int chunkXMin = Math.floorDiv(originX, 16);
        int chunkXMax = Math.floorDiv(originX+PlotUtils.PLOT_DIM-1, 16);
        int chunkZMin = Math.floorDiv(originZ, 16);
        int chunkZMax = Math.floorDiv(originZ+PlotUtils.PLOT_DIM-1, 16);
        for (int chunkX = chunkXMin; chunkX <= chunkXMax; chunkX++) {
            for (int chunkZ = chunkZMin; chunkZ <= chunkZMax; chunkZ++) {
                if (!bot.getWorld().isLoaded(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean isInStartingPosition() {
        if (bot.getPosManager().getY() < PlotUtils.PLOT_DIM) {
            return false;
        }
        double dx = (originX + PlotUtils.PLOT_DIM/2) - bot.getPosManager().getX();
        double dz = (originZ+ PlotUtils.PLOT_DIM/2) - bot.getPosManager().getZ();
        if (dx*dx + dz*dz > 4) {
            return false;
        } else {
            return true;
        }
    }

    void sendStartingPositionTpCommand() {
        bot.sendCommand(String.format(
                "/tp %d %d %d",
                originX + PlotUtils.PLOT_DIM/2,
                PlotUtils.PLOT_DIM,
                originZ + PlotUtils.PLOT_DIM/2)
        );
    }

    void processAction() {
        Action action = actionQueue.peek();
        if (action.getActionType() == ActionType.MOVE) {
            processMove((MoveAction) action);
        } else if (action.getActionType() == ActionType.HOLD) {
            processHold((HoldAction) action);
        } else if (action.getActionType() == ActionType.PLACE) {
            processPlace((PlaceAction) action);
        } else if (action.getActionType() == ActionType.WE_COMMAND) {
            processWECommand((WECommandAction) action);
        } else if (action.getActionType() == ActionType.AWAIT_CHUNKS) {
            processAwaitChunks();
        } else if (action.getActionType() == ActionType.WAIT) {
            processWait((WaitAction) action);
        }
    }

    void processMove(MoveAction action) {
        PositionManager posManager = bot.getPosManager();
        Vec3d actionPos = new Vec3d(action.x, action.y, action.z);
        Vec3d currentPos = new Vec3d(posManager.getX(), posManager.getY(), posManager.getZ());
        Vec3d targetPos;
        if (currentPos.y != action.y) {
            if (Math.abs(action.y - currentPos.y) <= 2.5) {
                targetPos = new Vec3d(currentPos.x, action.y, currentPos.z);
            } else if (action.y > currentPos.y) {
                targetPos = new Vec3d(currentPos.x, currentPos.y+2.5, currentPos.z);
            } else {
                targetPos = new Vec3d(currentPos.x, currentPos.y-2.5, currentPos.z);
            }
        } else {
            Vec3d difVec = actionPos.subtract(currentPos);
            if (difVec.lengthSquared() > 9) {
                targetPos = currentPos.add(difVec.normalize().multiply(3));
            } else {
                targetPos = actionPos;
            }
        }
        // Block intersection bounds
        World world = bot.getWorld();
        int minIX = (int) Math.floor(targetPos.x - 0.3);
        int maxIX = (int) Math.floor(targetPos.x + 0.3);
        int minIZ = (int) Math.floor(targetPos.z - 0.3);
        int maxIZ = (int) Math.floor(targetPos.z + 0.3);
        int minIY = (int) Math.floor(targetPos.y);
        int maxIY = (int) Math.floor(targetPos.y + 1.8);
        for (int y = minIY; y <= maxIY; y++) {
            for (int z = minIZ; z <= maxIZ; z++) {
                for (int x = minIX; x <= maxIX; x++) {
                    if (!BlockUtils.isAir(world.getBlock(x, y, z))) {
                        world.breakBlock(x, y, z);
                        return;
                    }
                }
            }
        }
        posManager.move(targetPos.x, targetPos.y, targetPos.z);
        if (targetPos.equals(actionPos)) {
            actionQueue.poll();
        }
    }

    void processHold(HoldAction action) {
        bot.sendPacket(new ServerboundSetCreativeModeSlotPacket(36, new ItemStack(action.itemId)));
        actionQueue.poll();
    }

    void processPlace(PlaceAction action) {
        if (bot.getWorld().inLoadedChunk(action.x, action.z)) {
            if (action.stateId == 0) {
                bot.getWorld().breakBlock(action.x, action.y, action.z);
            } else {
                bot.getWorld().placeBlock(action.x, action.y, action.z, action.stateId);
                if (!section.getBlock(action.x - originX, action.y, action.z - originZ).equals("air")) {
                    currentLayerProgress++;
                }
            }
            actionQueue.poll();
        }
    }

    void processWECommand(WECommandAction action) {
        // If awaitWE, don't immediately poll command and wait for WEFinished flag
        // WECooldown will cause it to re-run the awaited command if it doesn't detect any updates in 10 time ticks
        if (action.awaitWE && WEFinished) {
            WECooldown = 1; // Set to 1 because CommandHandler will already guarantee a minimum 1000 delay
            WEFinished = false;
            actionQueue.poll();
        } else if (WECooldown <= 0) {
            bot.sendCommand(action.command);
            if (!action.awaitWE) {
                WECooldown = 1; // Set to 1 because CommandHandler will already guarantee a minimum 1000 delay
                actionQueue.poll();
            } else {
                WECooldown = 10;
            }
        }
    }

    void processAwaitChunks() {
        if (allChunksLoaded()) {
            actionQueue.poll();
        }
    }

    void processWait(WaitAction action) {
        if (!waiting) {
            waiting = true;
            waitTime = 0;
        } else if (waitTime >= action.duration) {
            waiting = false;
            waitTime = 0;
            actionQueue.poll();
        }
    }

    int getFirstLayerDifference() {
        int[] builtCache = new int[LAYER_LIMIT+1];
        for (int layer = LAYER_LIMIT-1; layer >= 0; layer--) {
            builtCache[layer] = Math.max(0, getLayerRequiredBlocks(layer)-getLayerDifferences(layer));
            if (layer < LAYER_LIMIT-1) {
                builtCache[layer] += builtCache[layer+1];
            }
        }
        for (int layer = 0; layer < LAYER_LIMIT; layer++) {
            int differences = getLayerDifferences(layer);
            if (differences > builtCache[layer+1] / 10) {
                return layer;
            } else {
//                removeDifferencesWithWorldEdit(layer);
            }
        }
        return LAYER_LIMIT;
    }

    int getLayerDifferences(int layer) {
        int differences = 0;
        for (int y = layer*2; y<=layer*2+1 && y < PlotUtils.PLOT_DIM; y++) {
            for (int z = 0; z < PlotUtils.PLOT_DIM; z++) {
                for (int x = 0; x < PlotUtils.PLOT_DIM; x++) {
                    int bx = x + originX;
                    int by = y;
                    int bz = z + originZ;
                    int blockState = bot.getWorld().getBlock(bx, by, bz);
                    String blockName = BlockUtils.getBlockByStateId(blockState).getName();
                    if (y == 0 && section.getBlock(x, y, z).equals("air") && blockName.equals(FLOOR_BLOCK)) {
                        continue;
                    } else if (!section.getBlock(x, y, z).equals(blockName)) {
                        differences++;
                    }
                }
            }
        }
        return differences;
    }

//    void removeDifferencesWithWorldEdit(int layer) {
//        for (int y = layer*2; y<=layer*2+1 && y < PlotUtils.PLOT_DIM; y++) {
//            for (int z = 0; z < PlotUtils.PLOT_DIM; z++) {
//                for (int x = 0; x < PlotUtils.PLOT_DIM; x++) {
//                    int bx = x + originX;
//                    int by = y;
//                    int bz = z + originZ;
//                    int blockState = bot.getWorld().getBlock(bx, by, bz);
//                    String blockName = BlockUtils.getBlockByStateId(blockState).getName();
//                    if (y == 0 && section.getBlock(x, y, z).equals("air") && blockName.equals(FLOOR_BLOCK)) {
//                        continue;
//                    } else if (!section.getBlock(x, y, z).equals(blockName)) {
//                        actionQueue.add(new WECommandAction(
//                                String.format("//pos1 %d,%d,%d", bx, by, bz),
//                                false));
//                        actionQueue.add(new WECommandAction(
//                                String.format("//pos2 %d,%d,%d", bx, by, bz),
//                                false));
//                        actionQueue.add(new WECommandAction(
//                                String.format("//set %s", section.getBlock(x, y, z)),
//                                true));
//                    }
//                }
//            }
//        }
//    }

    void doInitialSets() {
        HashSet<Integer> requiredLayers = new HashSet<>();
        for (int y = currentLayer*2; y < PlotUtils.PLOT_DIM; y++) {
            for (int z = 0; z < PlotUtils.PLOT_DIM; z++) {
                for (int x = 0; x < PlotUtils.PLOT_DIM; x++) {
                    int bx = x + originX;
                    int by = y;
                    int bz = z + originZ;
                    int blockState = bot.getWorld().getBlock(bx, by, bz);
                    String blockName = BlockUtils.getBlockByStateId(blockState).getName();
                    // You can have correct blocks existing in the current layer, but layers above that must be empty.
                    if (!blockName.equals("air") && (!section.getBlock(x, y, z).equals(blockName) || y/2 != currentLayer)) {
                        requiredLayers.add(y/2);
                    }
                }
            }
        }
        for (int layer : requiredLayers) {
            actionQueue.add(new WECommandAction(
                    String.format("//pos1 %d,%d,%d", originX, layer*2, originZ),
                    false));
            actionQueue.add(new WECommandAction(
                    String.format("//pos2 %d,%d,%d", originX+PlotUtils.PLOT_DIM-1, layer*2 + 1, originZ+PlotUtils.PLOT_DIM-1),
                    false));
            actionQueue.add(new WECommandAction(
                    "//set air",
                    true));
        }
    }

    public static final int GROUP_DIM = (PlotUtils.PLOT_DIM-1) / 8 + 1;
    @RequiredArgsConstructor
    class BlockGroup {
        public final int x;
        public final int z;
        public ArrayList<BlockChangeEntry> blockChangeEntries = new ArrayList<>();
        // If PLOT_DIM is not divisible by 8, the move pos may go beyond plot boundaries.
        // This will cause the in-bounds checker to make the bot try to teleport back,
        // so I made this quick function to prevent that
        int getMoveX() {
            if (8*x + 4 >= PlotUtils.PLOT_DIM) {
                return originX + 8*x;
            } else {
                return originX + 8*x + 4;
            }
        }
        int getMoveZ() {
            if (8*z + 4 >= PlotUtils.PLOT_DIM) {
                return originZ + 8*z;
            } else {
                return originZ + 8*z + 4;
            }
        }
    }
    @RequiredArgsConstructor
    class BlockChangeEntry {
        public final int x;
        public final int y;
        public final int z;
        public final String name;
        public final boolean shouldBreak;
    }
    void loadLayer() {
        World world = bot.getWorld();
        HashMap<Integer, BlockGroup> groupMap = new HashMap<>();

        for (int y = 0; y < 2 && currentLayer*2 + y < PlotUtils.PLOT_DIM; y++) {
            for (int z = 0; z < PlotUtils.PLOT_DIM; z++) {
                for (int x = 0; x < PlotUtils.PLOT_DIM; x++) {
                    String blockName = BlockUtils.getBlockByStateId(world.getBlock(originX + x, currentLayer*2 + y, originZ + z)).getName();
                    String targetBlockName = section.getBlock(x, currentLayer*2 + y, z);
                    if (!blockName.equals(targetBlockName)) {
                        int groupX = x / 8;
                        int groupZ = z / 8;
                        int groupIdx = groupX + groupZ*GROUP_DIM;
                        if (!groupMap.containsKey(groupIdx)) {
                            groupMap.put(groupIdx, new BlockGroup(groupX, groupZ));
                        }
                        BlockGroup group = groupMap.get(groupIdx);
                        boolean shouldBreak = !blockName.equals("air"); // If not originally air, needs to break first.
                        group.blockChangeEntries.add(new BlockChangeEntry(originX + x, currentLayer*2 + y, originZ + z, targetBlockName, shouldBreak));
                    }
                }
            }
        }

        // Greedy travelling salesman approximation
        ArrayList<BlockGroup> blockGroups = new ArrayList<>(groupMap.values());
        int lastX = originX + PlotUtils.PLOT_DIM/2;
        int lastZ = originZ + PlotUtils.PLOT_DIM/2;
        while (!blockGroups.isEmpty()) {
            int finalLastX = lastX;
            int finalLastZ = lastZ;
            BlockGroup closestGroup = blockGroups.stream().min((a, b) -> {
                double dxA = a.getMoveX() - finalLastX;
                double dzA = a.getMoveZ() - finalLastZ;
                double distA2 = dxA*dxA + dzA*dzA;
                double dxB = b.getMoveX() - finalLastX;
                double dzB = b.getMoveZ() - finalLastZ;
                double distB2 = dxB*dxB + dzB*dzB;
                return Double.compare(distA2, distB2);
            }).get();
            blockGroups.remove(closestGroup);
            lastX = closestGroup.getMoveX();
            lastZ = closestGroup.getMoveZ();
            actionQueue.add(new MoveAction(closestGroup.getMoveX(), currentLayer*2 + 2, closestGroup.getMoveZ()));
            Map<String, List<BlockChangeEntry>> processedEntries = closestGroup.blockChangeEntries
                    .stream()
                    .filter(bce -> bce.name.equals("air") || ItemUtils.getItemByName(bce.name) != null)
                    .sorted(Comparator.comparing(bce -> bce.name))
                    .collect(Collectors.groupingBy(bce -> bce.name));
            for (Map.Entry<String, List<BlockChangeEntry>> entry : processedEntries.entrySet()) {
                if (!entry.getKey().equals("air")) {
                    int itemId = ItemUtils.getItemByName(entry.getKey()).getId();
                    actionQueue.add(new HoldAction(itemId));
                }
                int blockState = BlockUtils.getBlockByName(entry.getKey()).getDefaultState();
                for (BlockChangeEntry bce : entry.getValue()) {
                    if (bce.shouldBreak) {
                        actionQueue.add(new PlaceAction(bce.x, bce.y, bce.z, 0));
                    }
                    if (blockState != 0) {
                        actionQueue.add(new PlaceAction(bce.x, bce.y, bce.z, blockState));
                    }
                }
            }
        }

        // Check for block updates
        actionQueue.add(new MoveAction(originX + PlotUtils.PLOT_DIM/2, currentLayer*2 + 2, originZ + PlotUtils.PLOT_DIM/2));
        actionQueue.add(new WaitAction(5));
    }

    void updateCurrentLayerProgress() {
        currentLayerProgress = 0;
        for (int y = currentLayer*2; y<=currentLayer*2+1 && y < PlotUtils.PLOT_DIM; y++) {
            for (int z = 0; z < PlotUtils.PLOT_DIM; z++) {
                for (int x = 0; x < PlotUtils.PLOT_DIM; x++) {
                    int bx = x + originX;
                    int by = y;
                    int bz = z + originZ;
                    int blockState = bot.getWorld().getBlock(bx, by, bz);
                    String blockName = BlockUtils.getBlockByStateId(blockState).getName();
                    if (section.getBlock(x, y, z).equals("air")) {
                        continue;
                    } else if (section.getBlock(x, y, z).equals(blockName)) {
                        currentLayerProgress++;
                    }
                }
            }
        }
    }

    int getLayerRequiredBlocks(int layer) {
        int needed = 0;
        for (int y = layer*2; y<=layer*2+1 && y < PlotUtils.PLOT_DIM; y++) {
            for (int z = 0; z < PlotUtils.PLOT_DIM; z++) {
                for (int x = 0; x < PlotUtils.PLOT_DIM; x++) {
                    if (!section.getBlock(x, y, z).equals("air")) {
                        needed++;
                    }
                }
            }
        }
        return needed;
    }

    public int getTotalRequiredBlocks() {
        int needed = 0;
        for (int i=0; i<LAYER_LIMIT; i++) {
            needed += getLayerRequiredBlocks(i);
        }
        return needed;
    }

    public int getCurrentProgress() {
        int progress = 0;
        for (int layer=0; layer<currentLayer; layer++) {
            progress += getLayerRequiredBlocks(layer);
        }
        progress += Math.min(currentLayerProgress, getLayerRequiredBlocks(currentLayer));
        return progress;
    }

    public void sendStatusMessage() {
        int totalBlocks = getTotalRequiredBlocks();
        int currentProgress = getCurrentProgress();
        bot.getChatQueue().sendChat("Currently building: " + plotName);
        bot.getChatQueue().sendChat(String.format(
                "This build requires %d block placements in total, and I've placed about %d of them so far, so I'm about %.2f%% done. Estimated build time remaining: %.2f hrs.",
                totalBlocks,
                currentProgress,
                (double) currentProgress / totalBlocks * 100.0,
                (totalBlocks - currentProgress) / 15. / 60. / 60.
        ));
    }

    public void stop(boolean finished) {
        this.finished = finished;
        this.stopped = true;
    }
}