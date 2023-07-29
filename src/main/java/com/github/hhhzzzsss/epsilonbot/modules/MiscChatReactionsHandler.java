package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.UUID;

@RequiredArgsConstructor
public class MiscChatReactionsHandler implements PacketListener {

	private final EpsilonBot bot;
	
	@Override
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundSystemChatPacket) {
			ClientboundSystemChatPacket t_packet = (ClientboundSystemChatPacket) packet;
			Component message = t_packet.getContent();
			String strMessage = ChatUtils.getFullText(message);
		}
		
	}
	
}
