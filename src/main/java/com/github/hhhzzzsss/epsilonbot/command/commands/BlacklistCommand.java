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
public class BlacklistCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "blacklist";
    }

    @Override
    public String[] getSyntax() {
        return new String[]{
                "[<username>]"
        };
    }

    @Override
    public String getDescription() {
        return "Adds someone to EpsilonBot's blacklist";
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
                if (ModerationManager.isBlacklisted(response.getUuid())) {
                    bot.sendResponse(response.getUsername() + " is already blacklisted", sender.getMsgSender());
                    return;
                }
                ModerationManager.blacklist(response.getUuid());
            } catch (Exception e) {
                bot.sendResponse("Error blacklisting: " + e.getMessage(), sender.getMsgSender());
                return;
            }

            try {
                ModerationManager.saveBlacklist();
            } catch (IOException e) {
                bot.sendResponse("Error saving blacklist: " + e.getMessage(), sender.getMsgSender());
                return;
            }
            bot.sendResponse("Successfully added " + response.getUsername() + " to the blacklist", sender.getMsgSender());
        })).start();
    }
}
