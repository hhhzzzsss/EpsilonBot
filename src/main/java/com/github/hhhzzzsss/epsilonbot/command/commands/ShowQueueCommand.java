package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartBuilderSession;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartCheckerThread;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

@RequiredArgsConstructor
public class ShowQueueCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "showqueue";
    }
    @Override
    public String[] getSyntax() {
        return new String[0];
    }
    @Override
    public String getDescription() {
        return "Shows the current mapart queue";
    }
    @Override
    public int getDefaultPermission() {
        return 0;
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        ArrayList<String> queuedItems = new ArrayList<>();
        if (bot.getBuildHandler().getBuilderSession() != null && bot.getBuildHandler().getBuilderSession() instanceof MapartBuilderSession) {
            MapartBuilderSession mbs = (MapartBuilderSession) bot.getBuildHandler().getBuilderSession();
            queuedItems.add("Current: " + truncateUrl(mbs.getUrl().toString()));
        }
        int index = 1;
        for (MapartCheckerThread mct : bot.getBuildHandler().getMapartQueue()) {
            queuedItems.add(index + ": " + truncateUrl(mct.getUrl().toString()));
            index++;
        }
        if (queuedItems.isEmpty()) {
            bot.sendChat("Queue is empty");
        } else {
            bot.sendChat(String.join(" | ", queuedItems));
        }
    }

    public String truncateUrl(String strUrl) {
        if (strUrl.startsWith("https://")) {
            strUrl = strUrl.substring(8);
        }
        if (strUrl.length() > 30) {
            String newUrl = strUrl.substring(0, 19) + "...";
            newUrl += strUrl.substring(strUrl.length() - 8);

            return newUrl;
        }
        return strUrl;
    }
}
