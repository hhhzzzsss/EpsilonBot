package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ArgsParser;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class InteractCommand extends ChatCommand {
    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "interact";
    }
    @Override
    public String[] getSyntax() {
        return new String[] {
                "[on|off]",
        };
    }
    @Override
    public String getDescription() {
        return "Toggles whether the personality module will be triggered by your chat messages";
    }
    @Override
    public int getDefaultPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        ArgsParser parser = new ArgsParser(this, args);
        String option = parser.readWord(false);
        parser.end();

        if (sender.getSenderPrefix() == null || sender.getSenderPrefix().length() == 0) {
            throw new CommandException("No sender prefix detected");
        }

        if (option == null) {
            bot.getPersonalityModule().toggleInteraction(sender.getSenderPrefix());
        } else if (option.equalsIgnoreCase("on")) {
            bot.getPersonalityModule().enableInteraction(sender.getSenderPrefix());
        } else if (option.equalsIgnoreCase("off")) {
            bot.getPersonalityModule().disableInteraction(sender.getSenderPrefix());
        } else {
            throw parser.getCustomError("Must say enable or disable");
        }
    }
}
