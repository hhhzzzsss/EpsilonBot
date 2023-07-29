package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.*;
import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PositionManager implements PacketListener, DisconnectListener {
	private final EpsilonBot bot;
	@Getter private double x = 0;
	@Getter private double y = 0;
	@Getter private double z = 0;
	@Getter private float yaw = 0;
	@Getter private float pitch = 0;
	@Getter private boolean spawned = false;
	
	public void move(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		bot.sendPacket(new ServerboundMovePlayerPosRotPacket(true, x, y, z, yaw, pitch));
	}
	
	public void moveLook(double x, double y, double z, float yaw, float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		bot.sendPacket(new ServerboundMovePlayerPosRotPacket(true, x, y, z, this.yaw, this.pitch));
	}
	
	public void look(float yaw, float pitch) {
		this.yaw = yaw;
		this.pitch = pitch;
		bot.sendPacket(new ServerboundMovePlayerPosRotPacket(true, x, y, z, this.yaw, this.pitch));
	}

	@Override
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundPlayerPositionPacket) {
			ClientboundPlayerPositionPacket t_packet = (ClientboundPlayerPositionPacket) packet;
			boolean[] relFlags = new boolean[5];
			for (PositionElement element : t_packet.getRelative()) {
				relFlags[element.ordinal()] = true;
			}
        	x = relFlags[0] ? x+t_packet.getX() : t_packet.getX();
        	y = relFlags[1] ? y+t_packet.getY() : t_packet.getY();
        	z = relFlags[2] ? z+t_packet.getZ() : t_packet.getZ();
        	yaw = relFlags[3] ? yaw+t_packet.getYaw() : t_packet.getYaw();
        	pitch = relFlags[4] ? pitch+t_packet.getPitch() : t_packet.getPitch();
        	bot.sendPacket(new ServerboundAcceptTeleportationPacket(t_packet.getTeleportId()));
        	spawned = true;
        }
	}
	
	@Override
	public void onDisconnected(DisconnectedEvent event) {
		spawned = false;
	}
}
