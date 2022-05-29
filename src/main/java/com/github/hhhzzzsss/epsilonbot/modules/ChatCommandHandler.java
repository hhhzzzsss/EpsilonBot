package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.*;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatCommandHandler implements PacketListener {
	private final EpsilonBot bot;
	private final CommandList commandList;
	@Getter private String prefix;
	@Getter private Pattern chatCommandPattern;
	@Getter private Pattern discordCommandPattern;
	
	public ChatCommandHandler(EpsilonBot bot, CommandList commandList, String prefix) {
		this.bot = bot;
		this.commandList = commandList;
		this.setPrefix(prefix);
	}
	
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundChatPacket) {
			ClientboundChatPacket t_packet = (ClientboundChatPacket) packet;
			Component message = t_packet.getMessage();
			String strMessage = ChatUtils.getFullText(message).replaceAll("§[0-9a-fklmnor]", "");
			UUID uuid = t_packet.getSenderUuid();
			
			if (uuid.equals(bot.getUuid())) {
				return;
			}
			
			Matcher m;
			String command;
			String args;
			if ((m = chatCommandPattern.matcher(strMessage)).matches()) {
				command = m.group(1);
				args = m.group(2).trim();
			} else if ((m = discordCommandPattern.matcher(strMessage)).matches()) {
				command = m.group(1);
				args = m.group(2).trim();
			} else {
				return;
			}
			
			try {
				runCommand(uuid, command, args);
			}
			catch (CommandException e) {
				bot.sendChat("Error: " + e.getMessage());
			}
		}
	}
	
	public void runCommand(UUID uuid, String commandName, String args) throws CommandException {
		Command command = commandList.get(commandName.toLowerCase());
		if (command == null) {
			throw new CommandException("Unknown command: " + commandName);
		}
		if (!(command instanceof ChatCommand)) {
			throw new CommandException("This command cannot be run from Minecraft chat");
		}
		int permission = Config.getConfig().getTrusted().contains(uuid.toString()) ? 1 : 0;
		if (command.getPermission() > permission) {
			throw new CommandException("You don't have permission to run this command");
		}
		
		((ChatCommand) command).executeChat(new ChatSender(uuid, permission), args);
	}

	/**
	 * Sets the prefix and compiles the a new command pattern using this prefix
	 *
	 * @param prefix The command prefix
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
		chatCommandPattern = Pattern.compile(String.format(".* » +%s(\\S+)(.*)?", prefix));
		discordCommandPattern = Pattern.compile(String.format("\\[Discord\\] .*: %s(\\S+)(.*)?", prefix));
	}
}
