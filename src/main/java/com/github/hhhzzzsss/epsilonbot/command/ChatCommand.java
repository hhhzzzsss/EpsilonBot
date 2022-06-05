package com.github.hhhzzzsss.epsilonbot.command;

public abstract class ChatCommand extends Command {
	public abstract void executeChat(ChatSender sender, String args) throws CommandException;
}
