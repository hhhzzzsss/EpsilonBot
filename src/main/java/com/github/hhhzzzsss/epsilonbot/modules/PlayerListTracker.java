package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.UUID;


public class PlayerListTracker implements PacketListener, DisconnectListener {

	@Getter private final HashMap<UUID, PlayerData> playerList = new HashMap<>();

	@Override
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundPlayerInfoPacket) {
			ClientboundPlayerInfoPacket t_packet = (ClientboundPlayerInfoPacket) packet;
			for (PlayerListEntry entry : t_packet.getEntries()) {
        		UUID uuid = entry.getProfile().getId();
				if (t_packet.getAction() == PlayerListEntryAction.ADD_PLAYER) {
            		playerList.put(uuid, PlayerData.fromEntry(entry));
				}
				else if (!playerList.containsKey(uuid)) {
					//System.err.println("Server tried to modify nonexistent player entry! This should not happen.");
					continue;
				}
				else if (t_packet.getAction() == PlayerListEntryAction.UPDATE_GAMEMODE) {
					playerList.get(uuid).setGameMode(entry.getGameMode());
				}
				else if (t_packet.getAction() == PlayerListEntryAction.UPDATE_LATENCY) {
					playerList.get(uuid).setPing(entry.getPing());
				}
				else if (t_packet.getAction() == PlayerListEntryAction.UPDATE_DISPLAY_NAME) {
					playerList.get(uuid).setDisplayName(entry.getDisplayName());
				}
				else if (t_packet.getAction() == PlayerListEntryAction.REMOVE_PLAYER) {
					playerList.remove(uuid);
				}
        	}
		}
	}
	
	public void onDisconnected(DisconnectedEvent event) {
		playerList.clear();
	}

	@AllArgsConstructor
	@Getter
	@Setter
	public static class PlayerData {
		private @NonNull GameProfile profile;
		private GameMode gameMode;
		private int ping;
		private Component displayName;

		public String getName() {
			return profile.getName();
		}

		public UUID getUUID() {
			return profile.getId();
		}

		public static PlayerData fromEntry(PlayerListEntry entry) {
			return new PlayerData(entry.getProfile(), entry.getGameMode(), entry.getPing(), entry.getDisplayName());
		}
	}
}
