package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.Command;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HelpCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "help";
    }
    @Override
    public String[] getSyntax() {
        return new String[] {"[<command>]"};
    }
    @Override
    public String getDescription() {
        return "Lists or explains commands";
    }
    @Override
    public int getDefaultPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        String prefix = bot.getChatCommandHandler().getPrefix();
        if (args.length() == 0) { // List commands
            StringBuilder sb = new StringBuilder();
            sb.append("Commands -");
            for (Command command : bot.getCommandList().getCommands()) {
                if (command instanceof ChatCommand && command.getPermission() <= sender.getPermission()) {
                    sb.append(" " + Config.getConfig().getCommandPrefix() + command.getName());
                }
            }
            bot.sendResponse(sb.toString(), sender.getMsgSender());
        } else {
            Command command = bot.getCommandList().get(args.split(" ", 2)[0].toLowerCase());
            if (command == null) {
                throw new CommandException("Unknown command: " + args);
            }
            StringBuilder sb = new StringBuilder();
            if (command.getPermission() > 0) {
                sb.append("[RESTRICTED] ");
            }
            sb.append(String.format("%s%s ", prefix, command.getName()));
            if (command.getSyntax().length == 0) {
                sb.append(String.format("- %s", command.getDescription()));
            }
            else if (command.getSyntax().length == 1) {
                sb.append(String.format("%s - %s", command.getSyntax()[0], command.getDescription()));
            }
            else {
                sb.append(String.format("- %s", command.getDescription()));
                for (String syntax : command.getSyntax()) {
                    sb.append(String.format("\n%s%s %s", prefix, command.getName(), syntax));
                }
            }
            if (command.getFlags().length > 0) {
                sb.append(String.format("\nFlags: %s", String.join(", ", command.getFlags())));
            }
            bot.sendResponse(sb.toString(), sender.getMsgSender());
        }
    }
}
