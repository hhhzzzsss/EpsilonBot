package com.github.hhhzzzsss.epsilonbot.mapart;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.util.DownloadUtils;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

public class MapartCheckerThread extends Thread {
    @Getter EpsilonBot bot;
    @Getter URL url;
    @Getter int horizDim;
    @Getter int vertDim;
    @Getter boolean dither;
    @Getter boolean useTransparency;
    @Getter Throwable exception;
    public MapartCheckerThread(EpsilonBot bot, URL url, int horizDim, int vertDim, boolean dither, boolean useTransparency) throws IOException {
        this.bot = bot;
        this.url = url;
        if (!this.url.getProtocol().startsWith("http")) {
            throw new IOException("Illegal protocol: must use http or https");
        }
        this.horizDim = horizDim;
        this.vertDim = vertDim;
        this.dither = dither;
        this.useTransparency = useTransparency;

        setDefaultUncaughtExceptionHandler((t, e) -> {
            exception = e;
        });
    }

    public void run() {
        BufferedImage img;
        try {
            byte[] imageBin = DownloadUtils.DownloadToByteArray(url, 50*1024*1024);
            if (!DownloadUtils.imageIsSafe(new ByteArrayInputStream(imageBin))) {
                throw new IOException("Image is too large");
            }
            img = ImageIO.read(DownloadUtils.DownloadToOutputStream(url, 50*1024*1024));
            if (useTransparency && !img.getColorModel().hasAlpha()) {
                throw new IOException("Failed to load as transparent image (PNG is recommended)");
            }
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
        return new MapartBuilderSession(bot, mapIdx, url, horizDim, vertDim, dither, useTransparency);
    }

    public MapartQueueState getQueueState() {
        MapartQueueState queueState = new MapartQueueState();
        queueState.url = this.url;
        queueState.horizDim = this.horizDim;
        queueState.vertDim = this.vertDim;
        queueState.dither = this.dither;
        queueState.useTransparency = this.useTransparency;
        return queueState;
    }
}
