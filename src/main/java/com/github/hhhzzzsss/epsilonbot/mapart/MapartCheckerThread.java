package com.github.hhhzzzsss.epsilonbot.mapart;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.util.DownloadUtils;
import lombok.Getter;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class MapartCheckerThread extends Thread {
    @Getter EpsilonBot bot;
    @Getter String strUrl;
    @Getter URL url;
    @Getter int horizDim;
    @Getter int vertDim;
    @Getter Throwable exception;
    public MapartCheckerThread(EpsilonBot bot, String strUrl, int horizDim, int vertDim) throws IOException {
        this.bot = bot;
        this.strUrl = strUrl;
        this.url = new URL(strUrl);
        if (!this.url.getProtocol().startsWith("http")) {
            throw new IOException("Illegal protocol: must use http or https");
        }
        this.horizDim = horizDim;
        this.vertDim = vertDim;

        setDefaultUncaughtExceptionHandler((t, e) -> {
            exception = e;
        });
    }

    public void run() {
        BufferedImage img;
        try {
            img = ImageIO.read(DownloadUtils.DownloadToOutputStream(url, 50*1024*1024));
        } catch (Exception e) {
            exception = e;
            return;
        }

        if (img == null) {
            exception = new IOException("Error: failed to load as image");
            return;
        }
    }

    public MapartBuilderSession getBuilderSession() throws IOException {
        int mapIdx = MapartManager.getMapartIndex().size();
        return new MapartBuilderSession(bot, mapIdx, strUrl, horizDim, vertDim);
    }
}
