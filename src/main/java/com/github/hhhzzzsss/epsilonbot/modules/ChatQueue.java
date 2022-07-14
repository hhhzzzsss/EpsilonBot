package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.listeners.TickListener;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ChatQueue implements TickListener, PacketListener {

	public final EpsilonBot bot;
	
	@Getter @Setter public long chatDelay = 1200;

	@Getter public long nextChatTime = System.currentTimeMillis();
	
	@Getter @Setter public int maxChatQueue = 12;
	
	public Queue<String> chatQueue = new LinkedBlockingQueue<>();
	boolean timeFlag = true;
	
	public void onTick() {
		long currentTime = System.currentTimeMillis();
		if (!chatQueue.isEmpty() && currentTime >= nextChatTime && timeFlag) {
			bot.sendPacket(new ServerboundChatPacket(chatQueue.poll()));
			nextChatTime = currentTime + chatDelay;
			timeFlag = false;
		}
	}

	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundSetTimePacket) {
			timeFlag = true;
		}
	}
	
	public static final Pattern chatSplitter = Pattern.compile("\\G\\s*([^\r\n]{1,256}(?=\\s|$)|[^\r\n]{256})");
	public void sendChat(String chat) {
		if (chatQueue.size() < maxChatQueue) {
			chat = stripInvalidChars(chat).trim().replace("§", "&");
			Matcher m = chatSplitter.matcher(chat);
			while (m.find()) {
				if (m.group(1).length() <= 256) {
					chatQueue.add(m.group(1));
				} else {
					chatQueue.add(m.group(1).substring(0, 256));
				}
			}
		}
	}
	
	public void sendCommand(String command) {
		command = stripInvalidChars(command).replace("§", "&");
		if (command.length() <= 256 && chatQueue.size() < maxChatQueue) {
			chatQueue.add(command);
		}
	}

	public String stripInvalidChars(String s) {
		StringBuilder stringBuilder = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (!isValidChar(c)) continue;
			stringBuilder.append(c);
		}
		return stringBuilder.toString();
	}

	public boolean isValidChar(char chr) {
		return chr != '\u00a7' && chr >= ' ' && chr != '\u007f';
	}
}
