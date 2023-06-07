package com.github.hhhzzzsss.epsilonbot.mapart;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MapartQueueState {
    public static final Path QUEUE_STATE_PATH = Path.of("queueStates.json");
    public static Gson gson = new Gson();

    public URL url;
    public int horizDim;
    public int vertDim;
    public boolean dither;
    public boolean useTransparency;

    public static boolean queueStatesExist() {
        return Files.exists(QUEUE_STATE_PATH);
    }

    public static List<MapartQueueState> loadQueueStates() throws IOException {
        if (Files.exists(QUEUE_STATE_PATH)) {
            Reader indexReader = Files.newBufferedReader(QUEUE_STATE_PATH);
            Type queueStateListType = new TypeToken<ArrayList<MapartQueueState>>(){}.getType();
            List<MapartQueueState> queueStates = gson.fromJson(indexReader, queueStateListType);
            indexReader.close();
            return queueStates;
        } else {
            return null;
        }
    }

    public static void saveQueueStates(List<MapartQueueState> states) throws IOException {
        Writer indexWriter = Files.newBufferedWriter(QUEUE_STATE_PATH);
        indexWriter.write(gson.toJson(states));
        indexWriter.close();
    }
}
