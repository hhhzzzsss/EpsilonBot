package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.*;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.hhhzzzsss.epsilonbot.util.HashUtils;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatCommandHandler implements PacketListener {
	private final EpsilonBot bot;
	private final CommandList commandList;
	@Getter private String prefix;
	@Getter private ArrayList<String> alternatePrefixes;
	@Getter private Pattern prefixPattern;
	@Getter private Pattern commandPattern;
	@Getter private Pattern msgCommandPattern;
	@Getter private Pattern chatCommandPattern;
	@Getter private Pattern discordCommandPattern;
	
	public ChatCommandHandler(EpsilonBot bot, CommandList commandList, String prefix, ArrayList<String> alternatePrefixes) {
		this.bot = bot;
		this.commandList = commandList;
		this.setPrefix(prefix, alternatePrefixes);
	}
	
	private void handleFullChat(Component message) {
		String strMessage = ChatUtils.getFullText(message).replaceAll("§[0-9a-fklmnor]", "");
		
		Matcher m;
		String command;
		String args;
		String msgSender = null;
		String username = null;
		if ((m = msgCommandPattern.matcher(strMessage)).matches()) {
			if (!m.group(2).equals("Freedom")) {
				return;
			}
			msgSender = m.group(1);
			username = msgSender;
			command = m.group(3);
			args = m.group(4);
		} else if ((m = chatCommandPattern.matcher(strMessage)).matches()) {
			username = m.group(1);
			command = m.group(2);
			args = m.group(3).trim();
			if (username.equalsIgnoreCase(bot.getUsername())) {
				return;
			}
		} else if ((m = discordCommandPattern.matcher(strMessage)).matches()) {
			command = m.group(1);
			args = m.group(2).trim();
		} else {
			return;
		}
		
		try {
			runCommand(new UUID(0, 0), username, msgSender, command, args);
		}
		catch (CommandException e) {
			if (msgSender == null) {
				bot.sendChat("Error: " + e.getMessage());
			} else {
				bot.sendMsg("Error: " + e.getMessage(), msgSender);
			}
		}
	}
	
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundPlayerChatPacket t_packet) {
			UUID uuid = t_packet.getSender();
			if (uuid.equals(bot.getUuid())) {
				return;
			}

			if (ModerationManager.isBlacklisted(uuid)) {
				return;
			}

			String strName = ChatUtils.getFullText(t_packet.getName()).replaceAll("§[0-9a-fklmnor]", "");
			String strMessage = t_packet.getContent();
			
			Matcher m;
			String command;
			String args;
			if ((m = commandPattern.matcher(strMessage)).matches()) {
				command = m.group(1);
				args = m.group(2).trim();
			} else {
				return;
			}

			try {
				runCommand(uuid, strName, null, command, args);
			} catch (CommandException e) {
				bot.sendChat("Error: " + e.getMessage());
			}
		} else if (packet instanceof ClientboundDisguisedChatPacket t_packet) {
			handleFullChat(t_packet.getMessage());
		} else if (packet instanceof ClientboundSystemChatPacket t_packet) {
			handleFullChat(t_packet.getContent());
		}
	}
	
	public void runCommand(UUID uuid, String username, String msgSender, String commandPrefixed, String args) throws CommandException {
		String commandName = prefixPattern.matcher(commandPrefixed).replaceAll("");
		if (commandName == null) {
			return;
		}
		Command command = commandList.get(commandName.toLowerCase());
		if (command == null) {
			throw new CommandException("Unknown command - " + commandName);
		}
		if (!(command instanceof ChatCommand)) {
			throw new CommandException("This command cannot be run from Minecraft chat");
		}

		int permission = 0;
		if (msgSender != null) {
			for (PlayerListTracker.PlayerData playerData : bot.getPlayerListTracker().getPlayerList().values()) {
				if (playerData.getName().equals(msgSender)) {
					if (Config.getConfig().getTrusted().contains(playerData.getUUID().toString())) {
						permission = 2;
					} else if (ModerationManager.isStaff(playerData.getUUID())) {
						permission = 1;
					}
					break;
				}
			}
		} else {
			if (Config.getConfig().getTrusted().contains(uuid.toString())) {
				permission = 2;
			} else if (ModerationManager.isStaff(uuid)) {
				permission = 1;
			}
		}
		
		String trustedKey = Config.getConfig().getTrustedKey();
		String staffKey = Config.getConfig().getStaffKey();
		
		int splitIdx = args.lastIndexOf(" ");
		String hash = splitIdx != -1 ? args.substring(splitIdx + 1) : args;
		String argsNoHash = splitIdx != -1 ? args.substring(0, splitIdx) : "";
		String commandNoHash = argsNoHash.isEmpty() ? commandPrefixed : commandPrefixed + " " + argsNoHash;
		if (!trustedKey.isEmpty() && HashUtils.isValidHash(commandNoHash, username, trustedKey, hash)) {
			args = argsNoHash;
			permission = 2;
		} else if (!staffKey.isEmpty() && HashUtils.isValidHash(commandNoHash, username, staffKey, hash)) {
			args = argsNoHash;
			permission = 1;
		}

		if (command.getPermission() > permission) {
			throw new CommandException("You don't have permission to run this command");
		}
		
		((ChatCommand) command).executeChat(new ChatSender(bot, uuid, msgSender, permission), args);
	}

	/**
	 * Sets the prefix and compiles a new command pattern using this prefix
	 *
	 * @param prefix The command prefix
	 */
	public void setPrefix(String prefix, ArrayList<String> alternatePrefixes) {
		this.prefix = prefix;
		this.alternatePrefixes = alternatePrefixes;
		ArrayList<String> allPrefixes = new ArrayList<>();
		allPrefixes.add(prefix);
		allPrefixes.addAll(alternatePrefixes);
		String prefixMatchingString = String.join("|", allPrefixes);

		prefixPattern = Pattern.compile(prefixMatchingString);
		commandPattern = Pattern.compile(String.format("((?:%s)\\S+)(.*)?", prefixMatchingString));
		msgCommandPattern = Pattern.compile(String.format("\\| (\\S+) \\((.+?)\\) > You: ((?:%s)\\S+)(.*)?", prefixMatchingString));
		chatCommandPattern = Pattern.compile(String.format(".*\\b(\\S+)\\s*(?: »|:) +((?:%s)\\S+)(.*)?", prefixMatchingString));
		discordCommandPattern = Pattern.compile(String.format("\\[Discord\\] .*: ((?:%s)\\S+)(.*)?", prefixMatchingString));
	}
}
