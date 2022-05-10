package com.github.hhhzzzsss.epsilonbot.buildsync;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.block.Section;
import com.github.hhhzzzsss.epsilonbot.buildsync.action.WECommandAction;
import com.github.hhhzzzsss.epsilonbot.util.BlockUtils;
import com.github.hhhzzzsss.epsilonbot.util.PlotUtils;

public class PlotRepairSession extends PlotBuilderSession {
    int totalDifferences = 0;

    public PlotRepairSession(EpsilonBot bot, Section section, PlotCoord plotCoord, String plotName) {
        super(bot, section, plotCoord, plotName);
    }

    @Override
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
                createRepairQueue();
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
            stop(true);
        }
    }

    void createRepairQueue() {
        for (int y = 0; y < PlotUtils.PLOT_DIM; y++) {
            for (int z = 0; z < PlotUtils.PLOT_DIM; z++) {
                for (int x = 0; x < PlotUtils.PLOT_DIM; x++) {
                    int bx = x + originX;
                    int by = y;
                    int bz = z + originZ;
                    int blockState = bot.getWorld().getBlock(bx, by, bz);
                    String blockName = BlockUtils.getBlockByStateId(blockState).getName();
                    String targetBlockName = section.getBlock(x, y, z);
                    if (y == 0 && targetBlockName.equals("air")) {
                        targetBlockName = FLOOR_BLOCK;
                    }
                    if (!blockName.equals(targetBlockName)) {
                        totalDifferences++;
                        actionQueue.add(new WECommandAction(
                                String.format("//pos1 %d,%d,%d", bx, by, bz),
                                false));
                        actionQueue.add(new WECommandAction(
                                String.format("//pos2 %d,%d,%d", bx, by, bz),
                                false));
                        actionQueue.add(new WECommandAction(
                                String.format("//set %s", section.getBlock(x, y, z)),
                                true));
                    }
                }
            }
        }
        bot.sendChat("Found " + totalDifferences + " blocks to fix");
    }

    @Override
    public void sendStatusMessage() {
        int repairedDifferences = totalDifferences - (int)Math.ceil(actionQueue.size() / 3.0);
        bot.getChatQueue().sendChat("Currently repairing: " + plotName);
        bot.getChatQueue().sendChat(String.format(
                "This build requires %d fixes in total, and I've fixed about %d of them so far, so I'm about %.2f%% done.",
                totalDifferences,
                repairedDifferences,
                (double) repairedDifferences / totalDifferences * 100.0
        ));
    }
}
