package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ModerationManager;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.listeners.TickListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Pattern;

// Format messages with &a&o
@RequiredArgsConstructor
public class PersonalityModule implements PacketListener, TickListener {
    public final EpsilonBot bot;

    private static final File logDir = new File("AI_logs");
    static {
        if (!logDir.exists()) {
            logDir.mkdir();
        }
    }

    @Getter private boolean enabled = false;
    @Getter private ScheduledExecutorService networkExecutor = null;
    private URI host = null;
    private HttpClient client = null;

    private File logFile = null;
    private OutputStreamWriter logWriter = null;

    private LinkedBlockingQueue<String> tokenizerQueue = new LinkedBlockingQueue<>();
    private List<List<Integer>> tokenizedLines = new ArrayList<>();
    private Set<String> interactivePrefixes = new CopyOnWriteArraySet<>();
    boolean responseFlag = false;

    private static final int PING_DELAY = 1000;
    private static final int QUEUE_CHECK_DELAY = 100;

    public void enable() throws IOException {
        if (!enabled) {
            try {
                host = new URI(Config.getConfig().getPersonalityServerHost());
            } catch (URISyntaxException e) {
                throw new IOException("Invalid personality server host in config");
            }
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            enabled = true;
            logFile = new File(logDir, (System.currentTimeMillis()/1000) + ".txt");
            logWriter = new OutputStreamWriter(new FileOutputStream(logFile, false), StandardCharsets.UTF_8);
            networkExecutor = Executors.newScheduledThreadPool(1);
            networkExecutor.execute(() -> verifyConnection(true));
            networkExecutor.scheduleWithFixedDelay(
                    () -> verifyConnection(false),
                    PING_DELAY,
                    PING_DELAY,
                    TimeUnit.MILLISECONDS
            );
            networkExecutor.scheduleWithFixedDelay(
                    () -> processTokenizerQueue(),
                    QUEUE_CHECK_DELAY,
                    QUEUE_CHECK_DELAY,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public void disable() {
        if (enabled) {
            if (networkExecutor != null) {
                networkExecutor.shutdownNow();
            } else {
                enabled = false;
                logFile = null;
            }
        }
    }

    public void toggleInteraction(String prefix) {
        if (interactivePrefixes.contains(prefix)) {
            disableInteraction(prefix);
        } else {
            enableInteraction(prefix);
        }
    }

    public void enableInteraction(String prefix) {
        prefix = prefix.replaceAll("\\(replying to .+?\\)", "");
        interactivePrefixes.add(prefix);
        bot.sendChat("Enabled interaction for: " + prefix, "&9");
    }

    public void disableInteraction(String prefix) {
        prefix = prefix.replaceAll("\\(replying to .+?\\)", "");
        interactivePrefixes.remove(prefix);
        bot.sendChat("Disabled interaction for: " + prefix, "&9");
    }

    private void verifyConnection(boolean firstPing) {
        JsonObject body = new JsonObject();
        body.addProperty("auth_token", Config.getConfig().getPersonalityServerAuthToken());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(host.resolve("ping"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 401) {
                bot.sendChatAsync("Access denied to personality module. Most likely, you didn't set the correct token in the config.", "&9");
                networkExecutor.shutdownNow();
            } else if (response.statusCode() != 200) {
                if (firstPing) {
                    bot.sendChatAsync("Could not connect to personality module", "&9");
                } else {
                    bot.sendChatAsync("Lost connection to personality module", "&9");
                }
                networkExecutor.shutdownNow();
            } else {
                if (firstPing) {
                    bot.sendChatAsync("Successfully connected to personality module", "&9");
                }
            }
        } catch (IOException e) {
            if (firstPing) {
                bot.sendChatAsync("Could not connect to personality module", "&9");
            } else {
                bot.sendChatAsync("Lost connection to personality module", "&9");
            }
            networkExecutor.shutdownNow();
        } catch (InterruptedException e) {}
    }

    private void processTokenizerQueue() {
        if (tokenizerQueue.isEmpty()) return;

        try {
            while (!tokenizerQueue.isEmpty()) {
                String strMessage = tokenizerQueue.peek();
                JsonObject body = new JsonObject();
                body.addProperty("auth_token", Config.getConfig().getPersonalityServerAuthToken());
                body.addProperty("text", strMessage);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(host.resolve("tokenize"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    List<Integer> tokens = JsonParser.parseString(response.body())
                            .getAsJsonArray().asList().stream()
                            .map(element -> element.getAsInt())
                            .toList();
                    tokenizedLines.add(tokens);
                    tokenizerQueue.poll();
                    logMessage(strMessage);
                    trimTokenizedLines();
                    if (strMessage.toLowerCase().contains("@epsilonbot")) {
                        responseFlag = true;
                    }
                    String strMessageWithReplyRemoved = strMessage.replaceAll(" \\(replying to .+?\\)", "");
                    for (String prefix : interactivePrefixes) {
                        if (strMessageWithReplyRemoved.startsWith(prefix)) {
                            responseFlag = true;
                        }
                    }
                } else {
                    bot.sendChatAsync("Lost connection to personality module", "&9");
                    networkExecutor.shutdownNow();
                    return;
                }
            }
            if (responseFlag) {
                responseFlag = false;
                generateResponse();
            }
        } catch (IOException e) {
            bot.sendChatAsync("Lost connection to personality module", "&9");
            networkExecutor.shutdownNow();
        } catch (InterruptedException e) {
        }
    }

    private void trimTokenizedLines() {
        int totalLength = tokenizedLines.stream()
                .map(list -> list.size())
                .reduce(0, Integer::sum);
        while (totalLength > 1024) {
            totalLength -= tokenizedLines.get(0).size();
            tokenizedLines.remove(0);
        }
    }

    private void generateResponse() throws IOException, InterruptedException {
        JsonArray jsonTokenArray = new JsonArray();
        tokenizedLines.stream()
                .flatMap(List::stream)
                .forEachOrdered(token -> jsonTokenArray.add(token));

        JsonObject body = new JsonObject();
        body.addProperty("auth_token", Config.getConfig().getPersonalityServerAuthToken());
        body.add("context", jsonTokenArray);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(host.resolve("generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            if (!tokenizerQueue.isEmpty()) {
                responseFlag = true;
                return;
            }
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            tokenizedLines.add(jsonObject.get("tokens").getAsJsonArray().asList().stream()
                    .map(element -> element.getAsInt())
                    .toList()
            );
            String responseText = jsonObject.get("text").getAsString();
            logMessage("EpsilonBot » " + responseText);
            bot.sendChatAsync(responseText, "&a&o");
            trimTokenizedLines();
        } else {
            throw new IOException();
        }
    }

    @Override
    public void onPacket(Packet packet) {
        if (packet instanceof ClientboundPlayerChatPacket t_packet) {
            UUID uuid = t_packet.getSender();
            if (uuid.equals(bot.getUuid())) {
                return;
            }
            if (ModerationManager.isBlacklisted(uuid)) {
                return;
            }

            handleChat(t_packet.getUnsignedContent());
        } else if (packet instanceof ClientboundDisguisedChatPacket t_packet) {
            handleChat(t_packet.getMessage());
        } else if (packet instanceof ClientboundSystemChatPacket t_packet) {
            handleChat(t_packet.getContent());
        }
    }

    private static final Pattern joinPattern = Pattern.compile("(\\S+)( \\(formerly known as \\S+\\))? joined the game\\.?");
    private static final Pattern leavePattern = Pattern.compile("(\\S+) left the game\\.?");
    private void handleChat(Component message) {
        if (!enabled) return;

        String strMessage = ChatUtils.getFullText(message);

        ArrayList<String> allPrefixes = new ArrayList<>();
        allPrefixes.add(Config.getConfig().getCommandPrefix());
        allPrefixes.addAll(Config.getConfig().getAlternatePrefixes());
        for (String prefix : allPrefixes) {
            if (strMessage.contains(": " + prefix) || strMessage.contains("» " + prefix)) {
                return;
            }
        }

        if (
                strMessage.contains(" » ")
                || strMessage.startsWith("[Discord] ")
                || joinPattern.matcher(strMessage).matches()
                || leavePattern.matcher(strMessage).matches()
        ) {
            tokenizerQueue.add(strMessage);
        }
    }

    @Override
    public void onTick() {
        if (networkExecutor != null && networkExecutor.isTerminated()) {
            networkExecutor = null;
            logFile = null;
            try {
                logWriter.flush();
                logWriter.close();
                logWriter = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            enabled = false;
            bot.sendChatAsync("Personality module has been disabled", "&9");
        }
    }

    public void logMessage(String message) {
        JsonObject msgObject = new JsonObject();
        msgObject.addProperty("text", message);
        try {
            logWriter.write(msgObject.toString() + '\n');
            logWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    @RequiredArgsConstructor
//    private class MessageToProcess {
//        public final String sender;
//        public final boolean containsMention;
//        public final String text;
//    }
}
