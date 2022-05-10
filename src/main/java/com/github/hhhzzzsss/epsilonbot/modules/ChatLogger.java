package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.Logger;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
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
		if (packet instanceof ClientboundLoginPacket) {
			log(String.format("Successfully logged in to %s:%d", bot.getHost(), bot.getPort()));
		}
		else if (packet instanceof ClientboundChatPacket) {
			ClientboundChatPacket t_packet = (ClientboundChatPacket) packet;
			String fullText = ChatUtils.getFullText(t_packet.getMessage());
			if (fullText.equals("") || fullText.startsWith("Command set: ") || fullText.matches("[\u2800-\u28FF\\s]+") || fullText.matches("[⬛\\s]{60,}")) {
				return;
			}
			log(fullText);
		}
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
