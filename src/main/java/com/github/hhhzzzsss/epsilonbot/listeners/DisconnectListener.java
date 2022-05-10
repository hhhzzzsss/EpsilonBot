package com.github.hhhzzzsss.epsilonbot.listeners;

import com.github.steveice10.packetlib.event.session.DisconnectedEvent;

public interface DisconnectListener {
	public void onDisconnected(DisconnectedEvent packet);
}
