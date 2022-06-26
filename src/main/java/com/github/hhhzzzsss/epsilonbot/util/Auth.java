package com.github.hhhzzzsss.epsilonbot.util;

import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;

public class Auth {
	private static final String OAUTH_CLIENT_ID = "389b1b32-b5d5-43b2-bddc-84ce938d6737"; // token from https://github.com/microsoft/Office365APIEditor

	public static MinecraftProtocol login(String username, String password, String type) throws Exception {
		switch (type) {
			case "microsoft":
				return loginMSA(username, password);
			case "mojang":
				return loginMojang(username, password);
			case "offline":
				return loginOffline(username);
			default:
				throw new Exception("Invalid auth type");
		}
	}

	public static MinecraftProtocol loginMSA(String username, String password) throws Exception {
		AuthenticationService authService = new MsaAuthenticationService(OAUTH_CLIENT_ID);
		authService.setUsername(username);
		authService.setPassword(password);
		authService.login();
		return new MinecraftProtocol(authService.getSelectedProfile(), authService.getAccessToken());
	}

	public static MinecraftProtocol loginMojang(String username, String password) throws Exception {
		AuthenticationService authService = new MojangAuthenticationService();
		authService.setUsername(username);
		authService.setPassword(password);
		authService.login();
		return new MinecraftProtocol(authService.getSelectedProfile(), authService.getAccessToken());
	}

	public static MinecraftProtocol loginOffline(String username) {
		return new MinecraftProtocol(username);
	}
}