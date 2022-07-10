package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.command.StaffManager;
import com.github.hhhzzzsss.epsilonbot.util.ProfileUtils;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.UUID;

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
                if (!StaffManager.isStaff(response.getUuid())) {
                    bot.sendChat(response.getUsername() + " is not a staff member");
                    return;
                }
                StaffManager.removeStaffMember(response.getUuid());
            } catch (Exception e) {
                bot.sendChat("Error removing staff: " + e.getMessage());
                return;
            }

            try {
                StaffManager.saveStaffList();
            } catch (IOException e) {
                bot.sendChat("Error saving staff list: " + e.getMessage());
                return;
            }
            bot.sendChat("Successfully removed " + response.getUsername() + " from staff");
        })).start();
    }
}
