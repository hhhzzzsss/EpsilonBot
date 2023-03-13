package com.github.hhhzzzsss.epsilonbot.util;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.exception.request.ServiceUnavailableException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.util.HTTP;
import com.microsoft.aad.msal4j.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

// The existing MsaAuthenticationService isn't working, so I made my own version
public class CustomMsaAuthenticationService extends AuthenticationService {
    private static final URI XBL_AUTH_ENDPOINT = URI.create("https://user.auth.xboxlive.com/user/authenticate");
    private static final URI XSTS_AUTH_ENDPOINT = URI.create("https://xsts.auth.xboxlive.com/xsts/authorize");
    private static final URI MC_LOGIN_ENDPOINT = URI.create("https://api.minecraftservices.com/authentication/login_with_xbox");
    private static final URI MC_PROFILE_ENDPOINT = URI.create("https://api.minecraftservices.com/minecraft/profile");
    private static final URI EMPTY_URI = URI.create("");
    private static final String CLIENT_ID = "389b1b32-b5d5-43b2-bddc-84ce938d6737"; // From https://github.com/microsoft/Office365APIEditor
    private static final String AUTHORITY = "https://login.microsoftonline.com/consumers";
    private static final Set<String> SCOPE = Set.of("XboxLive.signin", "offline_access");
    private PublicClientApplication app;

    public CustomMsaAuthenticationService() {
        super(EMPTY_URI);
        TokenCacheAspect tokenCacheAspect = new TokenCacheAspect("msaCache");
        try {
            app = PublicClientApplication
                    .builder(this.CLIENT_ID)
                    .authority(this.AUTHORITY)
                    .setTokenCacheAccessAspect(tokenCacheAspect)
                    .build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Device code not formatted correctly");
        }
    }

    private McLoginResponse getLoginResponseFromToken(String accessToken) throws RequestException {
        XblAuthRequest xblRequest = new XblAuthRequest(accessToken);
        XblAuthResponse response = (XblAuthResponse)HTTP.makeRequest(this.getProxy(), XBL_AUTH_ENDPOINT, xblRequest, XblAuthResponse.class);
        XstsAuthRequest xstsRequest = new XstsAuthRequest(response.Token);
        response = (XblAuthResponse)HTTP.makeRequest(this.getProxy(), XSTS_AUTH_ENDPOINT, xstsRequest, XblAuthResponse.class);
        McLoginRequest mcRequest = new McLoginRequest(response.DisplayClaims.xui[0].uhs, response.Token);
        return (McLoginResponse)HTTP.makeRequest(this.getProxy(), MC_LOGIN_ENDPOINT, mcRequest, McLoginResponse.class);
    }

    private void getProfile() throws RequestException {
        Map<String, String> headers = new HashMap();
        headers.put("Authorization", "Bearer " + this.accessToken);
        McProfileResponse response = (McProfileResponse)HTTP.makeRequest(this.getProxy(), MC_PROFILE_ENDPOINT, (Object)null, McProfileResponse.class, headers);
        this.selectedProfile = new GameProfile(response.id, response.name);
        this.profiles = Collections.singletonList(this.selectedProfile);
        this.username = response.name;
    }

    public void login() throws RequestException {
        boolean username = this.username != null && !this.username.isEmpty();
        if (!username) {
            throw new InvalidCredentialsException("Username is required");
        }

        Set<IAccount> accountsInCache = app.getAccounts().join();
        IAccount account = null;
        Iterator<IAccount> cacheItr = accountsInCache.iterator();
        while (cacheItr.hasNext()) {
            IAccount nextAcc = cacheItr.next();
            if (nextAcc.username().equals(this.username)) {
                System.out.println("Found account in cache with username " + this.username);
                account = nextAcc;
            }
        }

        IAuthenticationResult result;
        try {
            if (account == null) {
                throw new NullPointerException("Account not found in cache");
            }

            SilentParameters silentParameters = SilentParameters
                    .builder(SCOPE, account)
                    .build();

            result = app.acquireTokenSilently(silentParameters).join();
        } catch (Exception e) {
            if (account == null || e.getCause() instanceof MsalException) {
                Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) ->
                        System.out.println(deviceCode.message());

                DeviceCodeFlowParameters parameters =
                        DeviceCodeFlowParameters
                                .builder(SCOPE, deviceCodeConsumer)
                                .build();

                result = app.acquireToken(parameters).join();

                if (!result.account().username().equals(this.username)) {
                    throw new InvalidCredentialsException("Signed in with the wrong account");
                }
            } else {
                throw new ServiceUnavailableException("Failed to get MSA token");
            }
        }

        McLoginResponse response = getLoginResponseFromToken("d=" + result.accessToken());
        if (response == null) {
            throw new RequestException("Invalid response received.");
        }
        this.accessToken = response.access_token;

        try {
            this.getProfile();
        } catch (RequestException var6) {
            if (this.username == null || this.username.isEmpty()) {
                this.username = response.username;
            }
        }

        this.loggedIn = true;
    }

    public void logout() throws RequestException {
        super.logout();
    }

    public String toString() {
        return "MsaAuthenticationService{clientId='" + this.CLIENT_ID + '\'' + ", authority='" + this.AUTHORITY + '\'' + ", accessToken='" + this.accessToken + '\'' + ", loggedIn=" + this.loggedIn + ", username='" + this.username + '\'' + ", password='" + this.password + '\'' + ", selectedProfile=" + this.selectedProfile + ", properties=" + this.properties + ", profiles=" + this.profiles + '}';
    }

    private static class McProfileResponse {
        public UUID id;
        public String name;
        public McProfileResponse.Skin[] skins;

        private McProfileResponse() {
        }

        private static class Skin {
            public UUID id;
            public String state;
            public URI url;
            public String variant;
            public String alias;

            private Skin() {
            }
        }
    }

    private static class McLoginResponse {
        public String username;
        public String[] roles;
        public String access_token;
        public String token_type;
        public int expires_in;

        private McLoginResponse() {
        }
    }

    private static class XblAuthResponse {
        public String IssueInstant;
        public String NotAfter;
        public String Token;
        public XblAuthResponse.DisplayClaims DisplayClaims;

        private XblAuthResponse() {
        }

        private static class Xui {
            public String uhs;

            private Xui() {
            }
        }

        private static class DisplayClaims {
            public XblAuthResponse.Xui[] xui;

            private DisplayClaims() {
            }
        }
    }

    private static class McLoginRequest {
        private String identityToken;

        protected McLoginRequest(String uhs, String identityToken) {
            this.identityToken = "XBL3.0 x=" + uhs + ";" + identityToken;
        }
    }

    private static class XstsAuthRequest {
        private String RelyingParty = "rp://api.minecraftservices.com/";
        private String TokenType = "JWT";
        private XstsAuthRequest.Properties Properties;

        protected XstsAuthRequest(String token) {
            this.Properties = new XstsAuthRequest.Properties(token);
        }

        private static class Properties {
            private String[] UserTokens;
            private String SandboxId;

            protected Properties(String token) {
                this.UserTokens = new String[]{token};
                this.SandboxId = "RETAIL";
            }
        }
    }

    private static class XblAuthRequest {
        private String RelyingParty = "http://auth.xboxlive.com";
        private String TokenType = "JWT";
        private XblAuthRequest.Properties Properties;

        protected XblAuthRequest(String accessToken) {
            this.Properties = new XblAuthRequest.Properties(accessToken);
        }

        private static class Properties {
            private String AuthMethod = "RPS";
            private String SiteName = "user.auth.xboxlive.com";
            private String RpsTicket;

            protected Properties(String accessToken) {
                this.RpsTicket = accessToken;
            }
        }
    }

    private class TokenCacheAspect implements ITokenCacheAccessAspect {
        private Path path;
        private String data;

        public TokenCacheAspect(String filename) {
            this.path = Path.of(filename);
            this.data = readDataFromFile();
        }

        @Override
        public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
            iTokenCacheAccessContext.tokenCache().deserialize(data);
        }

        @Override
        public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
            data = iTokenCacheAccessContext.tokenCache().serialize();
            writeDataToFile();
        }

        private String readDataFromFile() {
            if (!Files.exists(path)) {
                return "";
            } else {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    System.out.println("Failed to read from MSA cache file: " + e.getMessage());
                    return "";
                }
            }
        }

        private void writeDataToFile() {
            try {
                Files.writeString(path, data, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.out.println("Failed to update MSA cache file: " + e.getMessage());
            }
        }
    }
}
