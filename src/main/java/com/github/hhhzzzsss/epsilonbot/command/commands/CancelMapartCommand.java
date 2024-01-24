package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ArgsParser;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartBuildState;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartCheckerThread;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class CancelMapartCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "cancelmapart";
    }
    @Override
    public String[] getSyntax() {
        return new String[]{
                "current|<index>",
        };
    }
    @Override
    public String getDescription() {
        return "Cancels a mapart build. Use current to cancel the current one or specify a queue index.";
    }
    @Override
    public int getDefaultPermission() {
        return 1;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        if (args.equalsIgnoreCase("current")) {
            if (MapartBuildState.buildStateExists()) {
                try {
                    MapartBuildState.deleteBuildState();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            bot.getBuildHandler().setBuilderSession(null);
            bot.sendResponse("Cancelled current mapart", sender.getMsgSender());
        } else {
            ArgsParser parser = new ArgsParser(this, args);
            int idx = parser.readInt(true);
            if (idx <= 0) {
                throw new CommandException("Index cannot be less than 1");
            }
            if (idx > bot.getBuildHandler().getMapartQueue().size()) {
                throw new CommandException("Index is larger than queue size");
            }
            int i=0;
            for (MapartCheckerThread mct : bot.getBuildHandler().getMapartQueue()) {
                i++;
                if (idx == i) {
                    bot.getBuildHandler().getMapartQueue().remove(mct);
                    break;
                }
            }
            bot.sendResponse("Removed from queue", sender.getMsgSender());
        }
    }
}
