package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ArgsParser;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartBuilderSession;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartCheckerThread;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartManager;
import com.github.hhhzzzsss.epsilonbot.modules.BuildHandler;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class MapartCommand extends ChatCommand {

    private final EpsilonBot bot;

    @Override
    public String getName() {
        return "mapart";
    }
    @Override
    public String[] getSyntax() {
        return new String[] {
                "<url> [<width>] [<height>] [<flags>]",
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
    @Override
    public String[] getFlags() {
        return new String[]{
                "--NO_DITHER",
                "--USE_TRANSPARENCY",
        };
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
        ArgsParser parser = new ArgsParser(this, args);

        AtomicBoolean dithering = new AtomicBoolean(true);
        AtomicBoolean useTransparency = new AtomicBoolean(false);
        parser.setFlagParser((String flag) -> {
            if (flag.equalsIgnoreCase("no_dither")) {
                dithering.set(false);
            } else if (flag.equalsIgnoreCase("use_transparency")) {
                useTransparency.set(true);
            } else {
                throw parser.getGenericError();
            }
        });

        String strUrl = parser.readWord(true);
        Integer width = parser.readInt(false);
        Integer height = parser.readInt(false);
        parser.end();

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
                mbs = new MapartBuilderSession(bot, mapIdx, url, width, height, dithering.get(), useTransparency.get(), sender.getMsgSender());
            } catch (IOException e) {
                throw new CommandException(e.getMessage());
            }
            buildHandler.setBuilderSession(mbs);
            bot.sendResponse("Loading mapart...", sender.getMsgSender());
            if (sender.getMsgSender() != null) {
                bot.sendChat(sender.getMsgSender() + " has privately requested a mapart");
            }
        } else {
            MapartCheckerThread mct;
            try {
                mct = new MapartCheckerThread(bot, url, width, height, dithering.get(), useTransparency.get(), sender.getMsgSender());
                if (sender.getMsgSender() != null) {
                    bot.sendChat(sender.getMsgSender() + " has privately requested a mapart");
                }
            } catch (IOException e) {
                throw new CommandException(e.getMessage());
            }
            buildHandler.queueMapart(mct);
        }
    }
}
