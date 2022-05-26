package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.block.Section;
import com.github.hhhzzzsss.epsilonbot.build.BuilderSession;
import com.github.hhhzzzsss.epsilonbot.buildsync.Plot;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotBuilderSession;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotManager;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.listeners.TickListener;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartBuildState;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartBuilderSession;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BuildHandler implements TickListener, PacketListener, DisconnectListener {
    public static final int ACTIONS_PER_TIME_PACKET = 20;

    public final EpsilonBot bot;
    @Getter @Setter BuilderSession builderSession = null;

    private int actionQuota = 0;

    public static final long BUILD_CHECK_DELAY = 5000;
    private long nextBuildCheckTime = System.currentTimeMillis();

    @Override
    public void onTick() {
        if (!bot.getStateManager().isOnFreedomServer()) {
            builderSession = null;
            return;
        }

        if (builderSession == null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime > nextBuildCheckTime) {
                checkMapart();
                if (builderSession == null) checkPlots();
                nextBuildCheckTime = currentTime + BUILD_CHECK_DELAY;
            }
        } else {
            if (actionQuota > 0) {
                builderSession.onAction();
                actionQuota--;
                if (builderSession.isStopped()) {
                    if (builderSession instanceof PlotBuilderSession) {
                        PlotBuilderSession plotBuilderSession = (PlotBuilderSession) builderSession;
                        PlotManager.getBuildStatus(plotBuilderSession.plotCoord).inProgress = false;
                        if (plotBuilderSession.isFinished()) {
                            PlotManager.getBuildStatus(plotBuilderSession.plotCoord).built = true;
                        }
                        try {
                            PlotManager.saveBuildStatus();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    builderSession = null;
                }
            }
        }
    }

    @Override
    public void onPacket(Packet packet) {
        if (!bot.getStateManager().isOnFreedomServer()) {
            builderSession = null;
            return;
        }

        if (packet instanceof ClientboundSetTimePacket) {
            actionQuota = ACTIONS_PER_TIME_PACKET;
            if (builderSession != null) {
                builderSession.onTimePacket();
                if (builderSession.isStopped()) {
                    builderSession = null;
                }
            }
        } else if (packet instanceof ClientboundChatPacket) {
            ClientboundChatPacket t_packet = (ClientboundChatPacket) packet;
            Component message = t_packet.getMessage();
            String strMessage = ChatUtils.getFullText(message);
            if (builderSession != null) {
                builderSession.onChat(strMessage);
                if (builderSession.isStopped()) {
                    builderSession = null;
                }
            }
        }
    }

    @Override
    public void onDisconnected(DisconnectedEvent event) {
        builderSession = null;
    }

    private void checkPlots() {
        List<Plot> sortedPlots = PlotManager.getPlotMap().values()
                .stream()
                .sorted((p1, p2) -> {
                    if (p1.getX() == p2.getX()) {
                        return Integer.compare(p1.getZ(), p2.getZ());
                    } else {
                        return Integer.compare(p1.getX(), p2.getX());
                    }
                })
                .collect(Collectors.toList());
        for (Plot plot : sortedPlots) {
            if (plot.isSaved() && PlotManager.getBuildStatus(plot.pos).isInProgress()) {
                loadPlot(plot);
                return;
            }
        }
        for (Plot plot : sortedPlots) {
            if (plot.isSaved() && !PlotManager.getBuildStatus(plot.pos).isBuilt()) {
                loadPlot(plot);
                return;
            }
        }
    }

    private void loadPlot(Plot plot) {
        try {
            PlotManager.getBuildStatus(plot.pos).setInProgress(true);
            PlotManager.saveBuildStatus();
            Section section = PlotManager.loadSchem(plot.pos);
            String name = plot.getName();
            setBuilderSession(new PlotBuilderSession(bot, section, plot.pos, name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkMapart() {
        if (MapartBuildState.buildStateExists()) {
            try {
                setBuilderSession(new MapartBuilderSession(bot, MapartBuildState.loadBuildState()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
