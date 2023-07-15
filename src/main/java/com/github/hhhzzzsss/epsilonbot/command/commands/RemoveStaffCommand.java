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
public class RemoveStaffCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "removestaff";
    }

    @Override
    public String[] getSyntax() {
        return new String[]{
                "[<username>]"
        };
    }

    @Override
    public String getDescription() {
        return "Removes someone from EpsilonBot's staff list";
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
                if (!ModerationManager.isStaff(response.getUuid())) {
                    bot.sendResponse(response.getUsername() + " is not a staff member", sender.getMsgSender());
                    return;
                }
                ModerationManager.removeStaffMember(response.getUuid());
            } catch (Exception e) {
                bot.sendResponse("Error removing staff: " + e.getMessage(), sender.getMsgSender());
                return;
            }

            try {
                ModerationManager.saveStaffList();
            } catch (IOException e) {
                bot.sendResponse("Error saving staff list: " + e.getMessage(), sender.getMsgSender());
                return;
            }
            bot.sendResponse("Successfully removed " + response.getUsername() + " from staff", sender.getMsgSender());
        })).start();
    }
}
