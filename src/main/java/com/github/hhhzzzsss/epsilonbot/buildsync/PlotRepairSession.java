package com.github.hhhzzzsss.epsilonbot.buildsync;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.block.Section;
import com.github.hhhzzzsss.epsilonbot.build.action.CommandAction;
import com.github.hhhzzzsss.epsilonbot.util.BlockUtils;
import com.github.hhhzzzsss.epsilonbot.util.PlotUtils;

import java.util.function.Consumer;

public class PlotRepairSession extends PlotBuilderSession {
    int totalDifferences = 0;

    public PlotRepairSession(EpsilonBot bot, Section section, PlotCoord plotCoord, String plotName) {
        super(bot, section, plotCoord, plotName);
    }

    @Override
    public void onAction() {
        // Teleport back if left plot
        if (!PlotUtils.isInPlot(bot.getPosManager().getX(), bot.getPosManager().getZ(), plotCoord)) {
            tryTeleport(originX + PlotUtils.PLOT_DIM/2, PlotUtils.PLOT_DIM, originZ + PlotUtils.PLOT_DIM/2, 20);
            return;
        }

        // Check for chunk loading
        if (!firstLoad) {
            if (allChunksLoaded() && bot.getPosManager().getY() >= PlotUtils.PLOT_DIM) {
                firstLoad = true;
                actionQueue.add(new CommandAction(
                        "//limit -1",
                        false));
                createRepairQueue();
            } else if (!isInStartingPosition()) {
                tryTeleport(originX + PlotUtils.PLOT_DIM/2, PlotUtils.PLOT_DIM, originZ + PlotUtils.PLOT_DIM/2, 20);
                return;
            } else {
                return;
            }
        }

        if (!actionQueue.isEmpty()) {
            processAction();
        } else {
            stop();
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
                        actionQueue.add(new CommandAction(
                                String.format("//pos1 %d,%d,%d", bx, by, bz),
                                false));
                        actionQueue.add(new CommandAction(
                                String.format("//pos2 %d,%d,%d", bx, by, bz),
                                false));
                        actionQueue.add(new CommandAction(
                                String.format("//set %s", section.getBlock(x, y, z)),
                                true));
                    }
                }
            }
        }
        bot.sendChat("Found " + totalDifferences + " blocks to fix");
    }

    @Override
    public void sendStatusMessage(Consumer<? super String> sendFunc) {
        int repairedDifferences = totalDifferences - (int)Math.ceil(actionQueue.size() / 3.0);
        sendFunc.accept("Currently repairing: " + plotName);
        sendFunc.accept(String.format(
                "This build requires %d fixes in total, and I've fixed about %d of them so far, so I'm about %.2f%% done.",
                totalDifferences,
                repairedDifferences,
                (double) repairedDifferences / totalDifferences * 100.0
        ));
    }
}
