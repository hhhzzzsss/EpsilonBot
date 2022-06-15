package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.modules.PlayerListTracker;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ListCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "list";
    }
    @Override
    public String[] getSyntax() {
        return new String[0];
    }
    @Override
    public String getDescription() {
        return "Lists players";
    }
    @Override
    public int getDefaultPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        int numPlayers = bot.getPlayerListTracker().getPlayerList().size();
        String playersString = String.join(
                ", ",
                bot.getPlayerListTracker().getPlayerList().values().stream().sorted().map(playerData -> playerData.getName()).collect(Collectors.toList())
        );
        bot.sendChat(String.format("There are %d/60 online players", numPlayers));
        bot.sendChat(playersString);
    }
}