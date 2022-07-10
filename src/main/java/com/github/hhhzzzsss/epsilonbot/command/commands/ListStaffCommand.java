package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.command.StaffManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListStaffCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "liststaff";
    }
    @Override
    public String[] getSyntax() {
        return new String[]{
                "[<username>]"
        };
    }
    @Override
    public String getDescription() {
        return "Lists people registered by EpsilonBot as staff";
    }
    @Override
    public int getDefaultPermission() {
        return 1;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        (new Thread(() -> {
            bot.sendChat("Current registered staff: " + String.join(", ", StaffManager.getStaffNames()));
        })).start();
    }
}
