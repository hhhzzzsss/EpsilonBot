package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.command.ModerationManager;
import com.github.hhhzzzsss.epsilonbot.util.ProfileUtils;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class UnblacklistCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "unblacklist";
    }
    @Override
    public String[] getSyntax() {
        return new String[]{
                "[<username>]"
        };
    }
    @Override
    public String getDescription() {
        return "Removes someone from EpsilonBot's blacklist";
    }
    @Override
    public int getDefaultPermission() {
        return 1;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        if (args.length() == 0) {
            throw new CommandException("Please specify a username");
        }

        (new Thread(() -> {
            ProfileUtils.PlayerProfileResponse response;
            try {
                response = ProfileUtils.getPlayerProfile(args);
                if (!ModerationManager.isBlacklisted(response.getUuid())) {
                    bot.sendChat(response.getUsername() + " is not blacklisted");
                    return;
                }
                ModerationManager.unblacklist(response.getUuid());
            } catch (Exception e) {
                bot.sendChat("Error blacklisting: " + e.getMessage());
                return;
            }

            try {
                ModerationManager.saveBlacklist();
            } catch (IOException e) {
                bot.sendChat("Error saving blacklist: " + e.getMessage());
                return;
            }
            bot.sendChat("Successfully removed " + response.getUsername() + " from the blacklist");
        })).start();
    }
}
