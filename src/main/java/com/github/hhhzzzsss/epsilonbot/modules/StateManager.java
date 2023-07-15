package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.listeners.TickListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.data.game.command.CommandNode;
import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.inventory.ClickItemAction;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientInformationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@RequiredArgsConstructor
public class StateManager implements PacketListener, TickListener, DisconnectListener {
	private final EpsilonBot bot;
	
	@Getter @Setter public static long commandDelay = 2000;
	private long nextRectifyTime = System.currentTimeMillis();

	@Getter @Setter private boolean isTotalFreedom = false;
	@Getter @Setter private boolean onFreedomServer = false;
	private long lastTimeOnServer = System.currentTimeMillis();
	private long nextServerJoinTime = System.currentTimeMillis();
	
	@Getter @Setter private boolean autoOp = true;
	@Getter private boolean opped = true;

	@Getter @Setter private boolean autoGamemode = true;
	@Getter @Setter private GameMode targetGamemode = GameMode.CREATIVE;
	@Getter private GameMode gamemode = GameMode.CREATIVE;

	@Getter @Setter private boolean autoWorld = true;
	@Getter @Setter private String targetWorld = "minecraft:flatlands";
	@Getter private String worldName = "minecraft:overworld";
	@Getter private int entityId = -1;
	
	@Override
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundGameEventPacket) {
			ClientboundGameEventPacket t_packet = (ClientboundGameEventPacket) packet;
        	if (t_packet.getNotification() == GameEvent.CHANGE_GAMEMODE) {
        		gamemode = (GameMode) t_packet.getValue();
        	}
		} else if (packet instanceof ClientboundLoginPacket) {
			ClientboundLoginPacket t_packet = (ClientboundLoginPacket) packet;
			entityId = t_packet.getEntityId();
			gamemode = t_packet.getGameMode();
			worldName = t_packet.getWorldName();

			// Send client information
			List<SkinPart> skinParts = new ArrayList<>();
			skinParts.add(SkinPart.CAPE);
			skinParts.add(SkinPart.JACKET);
			skinParts.add(SkinPart.LEFT_SLEEVE);
			skinParts.add(SkinPart.RIGHT_SLEEVE);
			skinParts.add(SkinPart.LEFT_PANTS_LEG);
			skinParts.add(SkinPart.RIGHT_PANTS_LEG);
			skinParts.add(SkinPart.HAT);
			bot.sendPacket(new ServerboundClientInformationPacket(
					"en-us", 16, ChatVisibility.FULL, true, skinParts, HandPreference.RIGHT_HAND, false, true));
		} else if (packet instanceof ClientboundEntityEventPacket) {
			ClientboundEntityEventPacket t_packet = (ClientboundEntityEventPacket) packet;
			if (t_packet.getEntityId() == entityId) {
				if (t_packet.getEvent() == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0) {
					opped = false;
				} else if (t_packet.getEvent() == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_4) {
					opped = true;
				}
			}
		} else if (packet instanceof ClientboundRespawnPacket) {
			ClientboundRespawnPacket t_packet = (ClientboundRespawnPacket) packet;
			gamemode = t_packet.getGamemode();
			worldName = t_packet.getWorldName();
		} else if (packet instanceof ClientboundCommandsPacket) {
			ClientboundCommandsPacket t_packet = (ClientboundCommandsPacket) packet;
			boolean wasOnFreedomServer = onFreedomServer;
			onFreedomServer = false;
			for (CommandNode node : t_packet.getNodes()) {
				if (node.getName() == null) {
					continue;
				}

				if (node.getName().equals("freedom-01")) {
					isTotalFreedom = true;
				} else if (node.getName().equals("opme")) {
					onFreedomServer = true;
				}
			}

			if (!isTotalFreedom) onFreedomServer = true;

			if (wasOnFreedomServer && !onFreedomServer) {
				lastTimeOnServer = System.currentTimeMillis();
			}
		} else if (packet instanceof ClientboundOpenScreenPacket) {
			ClientboundOpenScreenPacket t_packet = (ClientboundOpenScreenPacket) packet;
			if (!onFreedomServer && ChatUtils.getFullText(t_packet.getTitle()).contains("Server Selector")) {
				bot.sendPacket(new ServerboundContainerClickPacket(
						t_packet.getContainerId(),
						0,
						0,
						ContainerActionType.CLICK_ITEM,
						ClickItemAction.LEFT_CLICK,
						new ItemStack(0),
						new TreeMap<>()
				));
			}
		} else if (packet instanceof ClientboundSetHealthPacket) {
			if (((ClientboundSetHealthPacket) packet).getHealth() <= 0.0) {
				bot.sendPacket(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
			}
		}
	}
	
	@Override
	public void onDisconnected(DisconnectedEvent event) {
		opped = true;
		gamemode = GameMode.CREATIVE;
		onFreedomServer = false;
	}

	@Override
	public void onTick() {
		if (System.currentTimeMillis() >= nextRectifyTime) {
			rectify();
		}
	}
	
	public void rectify() {
		long currentTime = System.currentTimeMillis();
		nextRectifyTime = currentTime;
		if (!onFreedomServer && currentTime >= nextServerJoinTime) {
			bot.sendPacket(new ServerboundSetCarriedItemPacket(0));
			bot.sendPacket(new ServerboundUseItemPacket(Hand.MAIN_HAND, 0));
			if (currentTime-lastTimeOnServer > 10*1000) {
				nextServerJoinTime = currentTime + 10*1000;
			}
			if (currentTime-lastTimeOnServer > 60*1000) {
				nextServerJoinTime = currentTime + 60*1000;
			}
			if (currentTime-lastTimeOnServer > 5*60*1000) {
				nextServerJoinTime = currentTime + 5*60*1000;
			}
			nextRectifyTime += commandDelay;
			return;
		}
		if (autoOp && !opped) {
			if (isTotalFreedom) bot.sendCommand("/opme");
			nextRectifyTime += commandDelay;
		}
		if (autoGamemode && gamemode != targetGamemode) {
			if (targetGamemode == GameMode.SURVIVAL) {
				bot.sendCommand("/gms");
			} else if (targetGamemode == GameMode.CREATIVE) {
				bot.sendCommand("/gmc");
			} else if (targetGamemode == GameMode.ADVENTURE) {
				bot.sendCommand("/gma");
			}
			// Spectator not supported
			nextRectifyTime += commandDelay;
		}
		if (isTotalFreedom && autoWorld && !worldName.equals(targetWorld)) {
			bot.sendCommand("/world " + targetWorld.replace("minecraft:", ""));
		}
	}
}
