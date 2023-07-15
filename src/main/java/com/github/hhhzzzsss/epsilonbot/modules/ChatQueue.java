package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.listeners.TickListener;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.BitSet;
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
			// TODO: Support chat signatures
			final String message = chatQueue.poll();

			if (message.startsWith("/")) {
				bot.sendPacket(new ServerboundChatCommandPacket(message.substring(1), System.currentTimeMillis(), 0, new ArrayList<>(), 0, new BitSet()));
			} else {
				bot.sendPacket(new ServerboundChatPacket(message, System.currentTimeMillis(), 0, null, 0, new BitSet()));
			}
			nextChatTime = currentTime + chatDelay;
			timeFlag = false;
		}
	}

	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundSetTimePacket) {
			timeFlag = true;
		}
	}
	
	public static final Pattern chatSplitter = Pattern.compile("\\G\\s*([^\\r\\n]{1,256}(?=\\s|$)|[^\\r\\n]{256})");

	public void sendChat(String chat) {
		if (chatQueue.size() < maxChatQueue) {
			chat = chat.trim().replace("§", "&");
			Matcher m = chatSplitter.matcher(chat);
			while (m.find()) {
				String chatPiece = stripInvalidChars(m.group(1));
				if (chatPiece.length() <= 256) {
					chatQueue.add(chatPiece);
				} else {
					chatQueue.add(chatPiece.substring(0, 256));
				}
				System.out.println(m.group(1));
			}
		}
	}

	public void sendMsg(String message, String targetPlayer) {
		if (chatQueue.size() < maxChatQueue) {
			message = message.trim().replace("§", "&");
			String msgPre = String.format("/msg %s ", targetPlayer);
			int limit = 256-msgPre.length();
			Pattern msgSplitter = Pattern.compile(String.format("\\G\\s*([^\\r\\n]{1,%d}(?=\\s|$)|[^\\r\\n]{%d})", limit, limit));
			Matcher m = msgSplitter.matcher(message);
			while (m.find()) {
				String chatPiece = msgPre + stripInvalidChars(m.group(1));
				if (chatPiece.length() <= 256) {
					chatQueue.add(chatPiece);
				} else {
					chatQueue.add(chatPiece.substring(0, 256));
				}
				System.out.println(m.group(1));
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
