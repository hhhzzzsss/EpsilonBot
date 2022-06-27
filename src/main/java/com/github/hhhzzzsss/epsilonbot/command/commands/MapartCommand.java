package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ArgsParser;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartBuilderSession;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartCheckerThread;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartManager;
import com.github.hhhzzzsss.epsilonbot.modules.BuildHandler;
import com.github.hhhzzzsss.epsilonbot.util.UUIDUtil;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MapartCommand extends ChatCommand {

    private final EpsilonBot bot;
    private final Map<ChatSender, Instant> lastCommand = new HashMap<>();
    private final int ratelimitMinutes = Config.getConfig().getMapartRatelimit();
    private Instant lastDiscordCommand = Instant.now();

    @Override
    public String getName() {
        return "mapart";
    }

    @Override
    public String[] getSyntax() {
        return new String[]{
            "<url> [<width>] [<height>] [<dithering>]",
        };
    }

    @Override
    public String getDescription() {
        return "Builds mapart for a given image";
    }

    @Override
    public int getDefaultPermission() {
        return 0;
    }

    private void checkRatelimit(ChatSender sender) throws CommandException {
        Instant now = Instant.now();
        Instant lastCommandExecution;

        if (sender.getUuid().equals(UUIDUtil.NIL_UUID)) {
            lastCommandExecution = lastDiscordCommand;
        } else {
            lastCommandExecution = lastCommand.getOrDefault(sender, Instant.MIN);
        }

        long difference = Duration.between(lastCommandExecution, now).toMinutes();

        if (difference > ratelimitMinutes) {
            throw new CommandException(String.format(
                "You may not execute more than one command in a period of %d minutes.",
                ratelimitMinutes));
        }

        if (sender.getUuid().equals(UUIDUtil.NIL_UUID)) {
            lastDiscordCommand = now;
        } else {
            lastCommand.put(sender, now);
        }
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        checkRatelimit(sender);

        ArgsParser parser = new ArgsParser(this, args);

        String strUrl = parser.readWord(true);
        Integer width = parser.readInt(false);
        Integer height = parser.readInt(false);
        String ditheringArg = parser.readWord(false);
        boolean dithering;
        if (ditheringArg == null) {
            dithering = true;
        } else if (ditheringArg.equalsIgnoreCase("true")) {
            dithering = true;
        } else if (ditheringArg.equalsIgnoreCase("false")) {
            dithering = false;
        } else {
            throw parser.getError("true or false");
        }
        if (strUrl.startsWith("data:")) {
            throw new CommandException("Cannot build mapart from data: URLs. Please upload the image to a image sharing site like https://imgur.com/ and use the URL from there.");
        }

        if (width == null) {
            width = 1;
            height = 1;
        } else if (height == null) {
            throw new CommandException("If you specify a width please specify a height as well");
        }

        if (width < 1 || height < 1) {
            throw new CommandException("Width and height must be positive");
        }

        if (sender.getPermission() == 0) {
            if (width > 3 || height > 3) {
                throw new CommandException("Width and height cannot exceed 3");
            }
        }

        BuildHandler buildHandler = bot.getBuildHandler();
        URL url;
        try {
            url = new URL(strUrl);
        } catch (IOException e) {
            throw new CommandException(e.getMessage());
        }



        if (buildHandler.getBuilderSession() == null) {
            int mapIdx = MapartManager.getMapartIndex().size();
            MapartBuilderSession mbs;
            try {
                mbs = new MapartBuilderSession(bot, mapIdx, url, width, height, dithering);
            } catch (IOException e) {
                throw new CommandException(e.getMessage());
            }
            buildHandler.setBuilderSession(mbs);
            bot.sendChat("Loading mapart...");
        } else {
            MapartCheckerThread mct;
            try {
                mct = new MapartCheckerThread(bot, url, width, height, dithering);
            } catch (IOException e) {
                throw new CommandException(e.getMessage());
            }
            buildHandler.queueMapart(mct);
        }
    }
}
