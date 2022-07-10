package com.github.hhhzzzsss.epsilonbot.util;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProfileUtils {
    public static Gson gson = new Gson();

//    @Data
//    public static class UsernameHistoryEntry {
//        String username;
//        @SerializedName("changed_at")
//        String changedAt;;
//    }
    @Data
    public static class PlayerProfileResponse {
        UUID uuid = null;
        String username = null;
//        @SerializedName("username_history")
//        ArrayList<UsernameHistoryEntry> usernameHistory;
    }
    public static PlayerProfileResponse getPlayerProfile(String identifier) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + identifier);
        InputStreamReader isr = new InputStreamReader(DownloadUtils.DownloadToOutputStream(url, 1024*1024));
        PlayerProfileResponse response = null;
        try {
            response = gson.fromJson(isr, PlayerProfileResponse.class);
        } catch (Exception e) {
            throw new IOException("Player not found");
        }
        if (response.getUuid() == null || response.getUsername() == null) {
            throw new IOException("Player not found");
        }
        return response;
    }

    @RequiredArgsConstructor
    private static class NameQueryThread extends Thread {
        private final UUID uuid;
        @Getter String result = null;

        @Override
        public void run() {
            System.out.println("run() called");
            try {
                result = ProfileUtils.getPlayerProfile(uuid.toString()).getUsername();
            } catch (Exception e) {}
        }
    }
    public static List<String> parallelNameQuery(List<UUID> uuids) {
        System.out.println("parallelNameQuery() called");
        List<NameQueryThread> threads = uuids.stream().map((uuid) -> new NameQueryThread(uuid)).collect(Collectors.toList());
        for (NameQueryThread thread : threads) {
            thread.start();
        }
        for (NameQueryThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {}
        }
        return threads.stream().map((thread) -> thread.getResult()).collect(Collectors.toList());
    }

}
