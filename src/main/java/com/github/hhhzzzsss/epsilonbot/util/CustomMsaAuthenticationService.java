package com.github.hhhzzzsss.epsilonbot.util;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.aad.msal4j.PublicClientApplication;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

// The existing MsaAuthenticationService isn't working, so I made my own version
public class CustomMsaAuthenticationService extends AuthenticationService {
//    private static final URI XBL_AUTH_ENDPOINT = URI.create("https://user.auth.xboxlive.com/user/authenticate");
//    private static final URI XSTS_AUTH_ENDPOINT = URI.create("https://xsts.auth.xboxlive.com/xsts/authorize");
//    private static final URI MC_LOGIN_ENDPOINT = URI.create("https://api.minecraftservices.com/authentication/login_with_xbox");
//    private static final URI MC_PROFILE_ENDPOINT = URI.create("https://api.minecraftservices.com/minecraft/profile");
//    private static final String CLIENT_ID = "00000000441cc96b"; // Custom app registration I made for EpsilonBot
//    private static final String AUTHORITY = "https://login.microsoftonline.com/consumers";
//    private static final Set<String> SCOPE = Set.of("XboxLive.signin", "offline_access");
    public static final Gson GSON = new Gson();
    private static final URI EMPTY_URI = URI.create("");
    private static final Path CACHE_PATH = Path.of("msaCache");
    private JsonObject msaCache = new JsonObject();
    private PublicClientApplication app;

    public CustomMsaAuthenticationService() {
        super(EMPTY_URI);
        loadCache();
    }

    private void loadCache() {
        if (Files.exists(CACHE_PATH)) {
            try {
                msaCache = JsonParser.parseReader(Files.newBufferedReader(CACHE_PATH, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveCache() {
        try {
            Files.writeString(CACHE_PATH, msaCache.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void login() throws RequestException {
        boolean username = this.username != null && !this.username.isEmpty();
        if (!username) {
            throw new InvalidCredentialsException("Username is required");
        }

        HttpClient httpClient = MinecraftAuth.createHttpClient();
        StepFullJavaSession.FullJavaSession javaSession = null;
        if (msaCache.has(this.username)) {
            try {
                javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(msaCache);
                javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, javaSession);
            } catch (Exception e) {
                System.out.println("Invalid token for " + this.username + ", will request new device code");
                javaSession = null;
            }
        }

        if (javaSession == null) {
            try {
                javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                    System.out.println("Go to " + msaDeviceCode.getDirectVerificationUri());
                }));
            } catch (Exception e) {
                throw new RequestException(e);
            }
        }

        JsonObject serializedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(javaSession);
        msaCache.add(this.username, serializedSession);
        saveCache();

        this.accessToken = javaSession.getMcProfile().getMcToken().getAccessToken();
        this.selectedProfile = new GameProfile(javaSession.getMcProfile().getId(), javaSession.getMcProfile().getName());
        this.profiles = Collections.singletonList(this.selectedProfile);
        this.username = javaSession.getMcProfile().getName();

        this.loggedIn = true;
    }

    public void logout() throws RequestException {
        super.logout();
    }

    public String toString() {
        return "CustomMsaAuthenticationService{accessToken='" + this.accessToken + '\'' + ", loggedIn=" + this.loggedIn + ", username='" + this.username + '\'' + ", password='" + this.password + '\'' + ", selectedProfile=" + this.selectedProfile + ", properties=" + this.properties + ", profiles=" + this.profiles + '}';
    }
}
