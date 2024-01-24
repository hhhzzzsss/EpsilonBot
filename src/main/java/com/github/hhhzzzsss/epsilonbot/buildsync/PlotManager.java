package com.github.hhhzzzsss.epsilonbot.buildsync;

import com.github.hhhzzzsss.epsilonbot.block.Section;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class PlotManager {
    public static final Path BUILD_SYNC_DIR = Path.of("buildsync");
    public static final Path INDEX_PATH = BUILD_SYNC_DIR.resolve("index.json");
    public static final Path STATUS_PATH = Path.of("buildstatus.json");

    public static Gson gson = new GsonBuilder()
            .registerTypeAdapter(PlotCoord.class, new PlotCoord.PlotCoordDeserializer())
            .create();

    @Getter @Setter
    public static class BuildStatus {
        public boolean inProgress = false;
        public boolean built = false;
    }
    @Getter public static HashMap<PlotCoord, Plot> plotMap = new HashMap<>();
    public static HashMap<PlotCoord, BuildStatus> buildStatusMap = new HashMap<>();

    static {
        BUILD_SYNC_DIR.toFile().mkdirs();
        try {
            loadIndex();
            loadBuildStatus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Section loadSchem(PlotCoord coord) throws IOException {
        Path schemPath = BUILD_SYNC_DIR.resolve(coord.toString() + ".schem");
        if (!Files.exists(schemPath)) {
            throw new IOException("File does not exist");
        }
        return Section.loadFromSchem(Files.newInputStream(schemPath));
    }

    public static void loadBuildStatus() throws IOException {
        if (Files.exists(STATUS_PATH)) {
            Reader indexReader = Files.newBufferedReader(STATUS_PATH);
            Type typeToken = new TypeToken<HashMap<PlotCoord, BuildStatus>>() { }.getType();
            buildStatusMap = gson.fromJson(indexReader, typeToken);
            indexReader.close();
        }
    }

    public static void saveBuildStatus() throws IOException {
        Writer indexWriter = Files.newBufferedWriter(STATUS_PATH);
        indexWriter.write(gson.toJson(buildStatusMap));
        indexWriter.close();
    }

    public static BuildStatus getBuildStatus(PlotCoord coord) {
        if (!buildStatusMap.containsKey(coord)) {
            buildStatusMap.put(coord, new BuildStatus());
        }
        return buildStatusMap.get(coord);
    }

    public static void loadIndex() throws IOException {
        if (Files.exists(INDEX_PATH)) {
            Reader indexReader = Files.newBufferedReader(INDEX_PATH);
            Type typeToken = new TypeToken<HashMap<PlotCoord, Plot>>() { }.getType();
            plotMap = gson.fromJson(indexReader, typeToken);
            indexReader.close();
        }
    }
}
