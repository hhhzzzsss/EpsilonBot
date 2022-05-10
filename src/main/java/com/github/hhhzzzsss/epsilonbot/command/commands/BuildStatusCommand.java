package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuildStatusCommand implements ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "buildstatus";
    }
    @Override
    public String[] getSyntax() {
        return new String[0];
    }
    @Override
    public String getDescription() {
        return "Gets current build status";
    }
    @Override
    public int getPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        if (bot.getPlotBuilder().getPlotBuilderSession() != null) {
            bot.getPlotBuilder().getPlotBuilderSession().sendStatusMessage();
        } else {
            bot.sendCommand("Nothing is currently being built");
        }
    }
}
