package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.Logger;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;

public class ChatLogger implements PacketListener, DisconnectListener {
	private final EpsilonBot bot;
	@Getter @Setter private boolean writeToFile;
	
	public ChatLogger(EpsilonBot bot, boolean writeToFile) {
		this.bot = bot;
		this.writeToFile = writeToFile;
	}

	@Override
	public void onPacket(Packet packet) {
		final String fullText;
		if (packet instanceof ClientboundLoginPacket) {
			fullText = String.format("Successfully logged in to %s:%d", bot.getHost(), bot.getPort());
		} else if (packet instanceof ClientboundSystemChatPacket t_packet) {
			fullText = ChatUtils.getFullText(t_packet.getContent());
			if (fullText.equals("") || fullText.startsWith("Command set: ")) {
				return;
			}
		} else if (packet instanceof ClientboundDisguisedChatPacket t_packet) {
			fullText = ChatUtils.getFullText(t_packet.getMessage());
		} else if (packet instanceof ClientboundPlayerChatPacket t_packet) {
			if (t_packet.getUnsignedContent() != null) {
				fullText = ChatUtils.getFullText(t_packet.getUnsignedContent());
			} else {
				fullText = "<" + ChatUtils.getFullText(t_packet.getName()) + "> " + t_packet.getContent();
			}
		} else {
			return;
		}

		log(fullText);
	}

	@Override
	public void onDisconnected(DisconnectedEvent packet) {
		log("Disconnected: " + packet.getReason());
	}
	
	public void log(String fullText) {
		System.out.println(fullText);
		if (writeToFile) Logger.log(fullText);
	}
}
