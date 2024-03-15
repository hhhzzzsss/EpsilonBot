package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ArgsParser;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class PersonalityCommand extends ChatCommand {
    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "personality";
    }
    @Override
    public String[] getSyntax() {
        return new String[] {
                "<enable|disable>",
        };
    }
    @Override
    public String getDescription() {
        return "Enables/disables personality module";
    }
    @Override
    public int getDefaultPermission() {
        return 2;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        ArgsParser parser = new ArgsParser(this, args);
        String option = parser.readWord(true);
        parser.end();

        if (option.equalsIgnoreCase("enable")) {
            if (!bot.getPersonalityModule().isEnabled()) {
                try {
                    bot.getPersonalityModule().enable();
                } catch (IOException e) {
                    throw new CommandException(e.getMessage());
                }
            } else {
                bot.sendChat("Personality module is already enabled", "&9");
            }
        } else if (option.equalsIgnoreCase("disable")) {
            if (bot.getPersonalityModule().isEnabled()) {
                bot.getPersonalityModule().disable();
            } else {
                bot.sendChat("Personality module is already disabled", "&9");
            }
        } else {
            throw parser.getCustomError("Must say enable or disable");
        }
    }
}
