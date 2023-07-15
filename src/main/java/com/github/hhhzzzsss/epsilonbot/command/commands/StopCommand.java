package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.Main;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StopCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String[] getSyntax() {
        return new String[0];
    }

    @Override
    public String getDescription() {
        return "Stops the bot";
    }

    @Override
    public int getDefaultPermission() {
        return 2;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        bot.sendChatInstantly("Stopping...");
        Main.stopBot();
    }
}
