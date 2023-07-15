package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuildStatusCommand extends ChatCommand {

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
    public int getDefaultPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        if (bot.getBuildHandler().getBuilderSession() != null) {
            bot.getBuildHandler().getBuilderSession().sendStatusMessage(msg -> bot.sendResponse(msg, sender.getMsgSender()));
        } else {
            bot.sendResponse("Nothing is currently being built", sender.getMsgSender());
        }
    }
}
