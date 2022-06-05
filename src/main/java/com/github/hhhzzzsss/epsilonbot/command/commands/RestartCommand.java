package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.Main;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RestartCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "restart";
    }
    @Override
    public String[] getSyntax() {
        return new String[0];
    }
    @Override
    public String getDescription() {
        return "Restarts the bot";
    }
    @Override
    public int getDefaultPermission() {
        return 1;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        bot.sendChatInstantly("Restarting...");
        Main.restartBot();
    }
}
