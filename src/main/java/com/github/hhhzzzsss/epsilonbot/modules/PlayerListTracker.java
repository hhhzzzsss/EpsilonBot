package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;


public class PlayerListTracker implements PacketListener, DisconnectListener {

	@Getter private final HashMap<UUID, PlayerData> playerList = new HashMap<>();

	@Override
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundPlayerInfoUpdatePacket) {
			ClientboundPlayerInfoUpdatePacket t_packet = (ClientboundPlayerInfoUpdatePacket) packet;
			EnumSet<PlayerListEntryAction> actions = t_packet.getActions();

			for (PlayerListEntry entry : t_packet.getEntries()) {
        		UUID uuid = entry.getProfile().getId();
				if (actions.contains(PlayerListEntryAction.ADD_PLAYER)) {
            		playerList.put(uuid, PlayerData.fromEntry(entry));
				}
				else if (!playerList.containsKey(uuid)) {
					//System.err.println("Server tried to modify nonexistent player entry! This should not happen.");
					continue;
				}
				else if (actions.contains(PlayerListEntryAction.UPDATE_GAME_MODE)) {
					playerList.get(uuid).setGameMode(entry.getGameMode());
				}
				else if (actions.contains(PlayerListEntryAction.UPDATE_LATENCY)) {
					playerList.get(uuid).setPing(entry.getLatency());
				}
				else if (actions.contains(PlayerListEntryAction.UPDATE_DISPLAY_NAME)) {
					playerList.get(uuid).setDisplayName(entry.getDisplayName());
				}
        	}
		} 
		else if (packet instanceof ClientboundPlayerInfoRemovePacket) {
			ClientboundPlayerInfoRemovePacket t_packet = (ClientboundPlayerInfoRemovePacket) packet;
			for (UUID uuid : t_packet.getProfileIds()) {
				playerList.remove(uuid);
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
			return new PlayerData(entry.getProfile(), entry.getGameMode(), entry.getLatency(), entry.getDisplayName());
		}
	}
}
