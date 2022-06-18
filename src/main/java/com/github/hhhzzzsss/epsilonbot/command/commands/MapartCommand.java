package com.github.hhhzzzsss.epsilonbot.command.commands;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.block.Section;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotManager;
import com.github.hhhzzzsss.epsilonbot.buildsync.PlotRepairSession;
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
import java.util.Map;
import java.net.URL;

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

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException {
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
