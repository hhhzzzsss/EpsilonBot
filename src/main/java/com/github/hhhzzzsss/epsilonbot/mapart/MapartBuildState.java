package com.github.hhhzzzsss.epsilonbot.mapart;

import com.google.gson.*;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MapartBuildState {
    public static final Path STATE_PATH = Path.of("mapartState.json");
    public static Gson gson = new GsonBuilder()
            .registerTypeAdapter(Color.class, new ColorSerializer())
            .registerTypeAdapter(Color.class, new ColorDeserializer())
            .create();

    public URL url;
    public boolean useTransparency = false;
    public String requester;
    public int mapIdx;
    public int originX;
    public int originZ;
    public BlockElevation[][] blocks;
    public int maxElevation;
    public int numTiles;
    public int tileIndex = 0;

    public static boolean buildStateExists() {
        return Files.exists(STATE_PATH);
    }

    public static MapartBuildState loadBuildState() throws IOException {
        if (Files.exists(STATE_PATH)) {
            Reader indexReader = Files.newBufferedReader(STATE_PATH);
            MapartBuildState mapartBuildState = gson.fromJson(indexReader, MapartBuildState.class);
            indexReader.close();
            return mapartBuildState;
        } else {
            return null;
        }
    }

    public static void saveBuildState(MapartBuildState state) throws IOException {
        Writer indexWriter = Files.newBufferedWriter(STATE_PATH);
        indexWriter.write(gson.toJson(state));
        indexWriter.close();
    }

    public static void deleteBuildState() throws IOException {
        Files.delete(STATE_PATH);
    }

    // idc about color when reloading so I'm just using a dummy serializer and deserializer to make gson happy
	public static class ColorSerializer implements JsonSerializer<Color> {
        @Override
        public JsonElement serialize(Color color, Type type, JsonSerializationContext context) throws JsonParseException {
            return new JsonPrimitive(0);
        }
    }

	public static class ColorDeserializer implements JsonDeserializer<Color> {
        @Override
        public Color deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            return Color.BLACK;
        }
    }
}
