package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.block.Section;
import com.github.hhhzzzsss.epsilonbot.buildsync.Plot;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotManager;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotRepairSession;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.modules.BuildHandler;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class RepairCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "repair";
    }
    @Override
    public String[] getSyntax() {
        return new String[] {
                "<plot name>",
        };
    }
    @Override
    public String getDescription() {
        return "Attempts to repair a specified plot";
    }
    @Override
    public int getDefaultPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        if (sender.getPermission() == 0) {
            bot.sendChat("Sorry, usage of the `repair command has been restricted due to complaints about EpsilonBot spamming commandspy for staff. If a build has been griefed, please notify a staff member instead and they should be able to roll it back.");
            return;
        }

        BuildHandler plotBuilder = bot.getBuildHandler();
        for (Plot plot : PlotManager.getPlotMap().values()) {
            if (plot.isSaved() && plot.getName().equalsIgnoreCase(args)) {
                if (!PlotManager.getBuildStatus(plot.pos).isBuilt()) {
                    throw new CommandException("Cannot repair a plot that has not been built yet");
                }
                try {
                    Section section = PlotManager.loadSchem(plot.pos);
                    if (plotBuilder.getBuilderSession() != null) {
                        bot.sendChat("Another build is in progress. Interrupting that build to repair \"" + plot.getName() + "\"...");
                    } else {
                        bot.sendChat("Repairing \"" + plot.getName() + "\"...");
                    }
                    plotBuilder.setBuilderSession(new PlotRepairSession(bot, section, plot.pos, plot.getName()));
                } catch (IOException e) {
                    throw new CommandException(e.getMessage());
                }
                return;
            }
        }
        throw new CommandException("Could not find a plot by the name \"" + args + "\"");
    }
}
