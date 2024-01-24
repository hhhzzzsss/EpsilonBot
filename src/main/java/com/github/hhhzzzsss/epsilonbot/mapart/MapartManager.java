package com.github.hhhzzzsss.epsilonbot.mapart;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class MapartManager {
    public static final Path INDEX_PATH = Path.of("mapartIndex.json");
    public static Gson gson = new Gson();
    @Getter private static ArrayList<MapartInfo> mapartIndex = new ArrayList<>();

    @AllArgsConstructor
    public static class MapartInfo {
        URL url;
        int horizDim;
        int vertDim;
    }

    static {
        try {
            loadMapartIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadMapartIndex() throws IOException {
        if (Files.exists(INDEX_PATH)) {
            Reader indexReader = Files.newBufferedReader(INDEX_PATH);
            Type typeToken = new TypeToken<ArrayList<MapartInfo>>() { }.getType();
            mapartIndex = gson.fromJson(indexReader, typeToken);
            indexReader.close();
        }
    }

    public static void saveMapartIndex() throws IOException {
        Writer indexWriter = Files.newBufferedWriter(INDEX_PATH);
        indexWriter.write(gson.toJson(mapartIndex));
        indexWriter.close();
    }
}
