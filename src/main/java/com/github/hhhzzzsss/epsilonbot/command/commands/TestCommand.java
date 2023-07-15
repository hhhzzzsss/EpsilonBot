package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TestCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String[] getSyntax() {
        return new String[0];
    }

    @Override
    public String getDescription() {
        return "A test command";
    }

    @Override
    public int getDefaultPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        bot.sendResponse("Test command received. Your permission level is " + sender.getPermission() + ".", sender.getMsgSender());
    }
}
