package com.github.hhhzzzsss.epsilonbot.command;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import lombok.Getter;
import lombok.Setter;

public class PlatformInfo {
	public enum Platform {
		MINECRAFT,
		CONSOLE, // Not supported yet
	}

	@Getter private final Platform platform;
	@Getter private EpsilonBot bot;
	@Getter private Command command;
	@Getter @Setter private String title;
	
	private PlatformInfo(Platform platform) {
		this.platform = platform;
	}
	
	public static PlatformInfo getMinecraft(EpsilonBot bot) {
		PlatformInfo platformInfo = new PlatformInfo(Platform.MINECRAFT);
		platformInfo.bot = bot;
		return platformInfo;
	}
	
	public void sendMessage(String message) {
		if (platform == Platform.MINECRAFT) {
			sendMinecraftMessage(message);
		} else if (platform == Platform.CONSOLE) {
			// todo
		}
	}
	
	public void sendResponseOnlyMessage(String message) {
		if (platform == Platform.MINECRAFT) {
			sendMinecraftMessage(message);
		} else if (platform == Platform.CONSOLE) {
			// todo
		}
	}
	
	public void sendMinecraftOnlyMessage(String message) {
		sendMinecraftMessage(message);
	}
	
	private void sendMinecraftMessage(String message) {
		bot.sendChat(message);
	}
}
