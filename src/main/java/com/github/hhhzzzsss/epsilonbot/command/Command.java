package com.github.hhhzzzsss.epsilonbot.command;

import lombok.Getter;
import lombok.Setter;

public abstract class Command {
	public abstract String getName();
	public abstract String[] getSyntax();
	public abstract String getDescription();
	public abstract int getDefaultPermission();
	public String[] getFlags() {
		return new String[]{};
	}

	public String[] getAliases() {
		return new String[0];
	}

	@Getter @Setter private int permission = getDefaultPermission();
}
