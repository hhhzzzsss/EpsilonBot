package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotManager;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class ReloadIndexCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "reloadindex";
    }
    @Override
    public String[] getSyntax() {
        return new String[0];
    }
    @Override
    public String getDescription() {
        return "Reloads the plot index";
    }
    @Override
    public int getDefaultPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        try {
            PlotManager.loadIndex();
            bot.sendChat("Reloaded plot index");
        } catch (IOException e) {
            throw new CommandException(e.getMessage());
        }
    }
}
