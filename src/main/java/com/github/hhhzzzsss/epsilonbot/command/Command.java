package com.github.hhhzzzsss.epsilonbot.command;

import lombok.Getter;
import lombok.Setter;

public abstract class Command {
	public abstract String getName();
	public abstract String[] getSyntax();
	public abstract String getDescription();
	public abstract int getDefaultPermission();

	@Getter @Setter private int permission = getDefaultPermission();
}
