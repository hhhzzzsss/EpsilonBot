package com.github.hhhzzzsss.epsilonbot.command;

import com.github.hhhzzzsss.epsilonbot.util.ProfileUtils;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StaffManager {
    public static final Path STAFF_JSON_PATH = Path.of("staff.json");
    public static Gson gson = new Gson();
    @Getter private static HashSet<UUID> staffList = new HashSet<>();

    static {
        try {
            loadStaffList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadStaffList() throws IOException {
        if (Files.exists(STAFF_JSON_PATH)) {
            Reader indexReader = Files.newBufferedReader(STAFF_JSON_PATH);
            Type typeToken = new TypeToken<HashSet<UUID>>() {}.getType();
            staffList = gson.fromJson(indexReader, typeToken);
            indexReader.close();
        }
    }

    public static void saveStaffList() throws IOException {
        Writer indexWriter = Files.newBufferedWriter(STAFF_JSON_PATH);
        indexWriter.write(gson.toJson(staffList));
        indexWriter.close();
    }

    public static void addStaffMember(UUID uuid) {
        staffList.add(uuid);
    }

    public static void removeStaffMember(UUID uuid) {
        staffList.remove(uuid);
    }

    public static boolean isStaff(UUID uuid) {
        return staffList.contains(uuid);
    }

    public static List<String> getStaffNames() {
        return ProfileUtils.parallelNameQuery(new ArrayList<>(staffList))
                .stream()
                .map((name) -> name == null ? "(error retreiving name)" : name)
                .collect(Collectors.toList());
    }


//    private static class AtomicIntegerTypeAdapter implements JsonSerializer<AtomicInteger>, JsonDeserializer<AtomicInteger> {
//        @Override public JsonElement serialize(AtomicInteger src, Type typeOfSrc, JsonSerializationContext context) {
//            return new JsonPrimitive(src.incrementAndGet());
//        }
//
//        @Override public AtomicInteger deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
//                throws JsonParseException {
//            int intValue = json.getAsInt();
//            return new AtomicInteger(--intValue);
//        }
//    }
}
