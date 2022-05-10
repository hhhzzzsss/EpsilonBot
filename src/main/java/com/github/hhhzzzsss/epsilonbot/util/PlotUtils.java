package com.github.hhhzzzsss.epsilonbot.util;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotBuilderSession;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotCoord;

public class PlotUtils {
    public static final int PLOT_DIM = 256;

    public static PlotCoord getPlotCoord(double x, double z) {
        int fx = (int) Math.floor(x);
        int fz = (int) Math.floor(z);
        return new PlotCoord(Math.floorDiv(fx, PLOT_DIM), Math.floorDiv(fz, PLOT_DIM));
    }

    public static boolean isInPlot(double x, double z, PlotCoord coord) {
        double worldX = coord.x*PLOT_DIM + Config.getConfig().getBuildSyncX();
        double worldZ = coord.z*PLOT_DIM + Config.getConfig().getBuildSyncZ();
        if (x >= worldX && x <= worldX+256 && z >= worldZ && z <= worldZ+256) {
            return true;
        } else {
            return false;
        }
    }
}
