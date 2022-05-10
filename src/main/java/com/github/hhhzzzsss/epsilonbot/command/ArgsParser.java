package com.github.hhhzzzsss.epsilonbot.command;

import com.github.hhhzzzsss.epsilonbot.block.BlockSelector;
import com.github.hhhzzzsss.epsilonbot.entity.EntitySelector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ArgsParser {
	private final Command command;
	private final String args;
	@Getter private int position = 0;
	
	public static final Pattern stringPattern = Pattern.compile("^\\s*(.+)", Pattern.DOTALL);
	public static final Pattern wordPattern = Pattern.compile("^\\s*(\\S+)");
	public static final Pattern flagPattern = Pattern.compile("^\\s*-([^\\s0-9]\\S*)");
	
	public String readString(boolean required) throws CommandException {
		String arg = findPattern(stringPattern);
		if (arg == null && required) {
			throw getError("string");
		}
		
		return arg;
	}
	
	public String readWord(boolean required) throws CommandException {
		String arg = findPattern(wordPattern);
		if (arg == null && required) {
			throw getGenericError();
		}
		
		return arg;
	}
	
	public Integer readInt(boolean required) throws CommandException {
		String arg = findPattern(wordPattern);
		if (arg == null) {
			if (required) {
				throw getError("integer");
			}
			else {
				return null;
			}
		}
		
		try {
			return Integer.parseInt(arg);
		}
		catch (NumberFormatException e) {
			throw getError("integer");
		}
	}
	
	public Double readDouble(boolean required) throws CommandException {
		String arg = findPattern(wordPattern);
		if (arg == null) {
			if (required) {
				throw getError("double");
			}
			else {
				return null;
			}
		}
		
		try {
			return Double.parseDouble(arg);
		}
		catch (NumberFormatException e) {
			throw getError("double");
		}
	}
	
	public <T extends Enum<T>> T readEnum(Class<T> enumType, boolean required) throws CommandException {
		String arg = findPattern(wordPattern);
		if (arg == null && required) {
			throw getError(enumType.getSimpleName());
		}
		
		try {
			return Enum.valueOf(enumType, arg.toUpperCase());
		}
		catch (IllegalArgumentException|NullPointerException e) {
			throw getError(enumType.getSimpleName());
		}
	}
	
	public EntitySelector readEntitySelector(boolean required) throws CommandException {
		String arg = findPattern(wordPattern);
		if (arg == null) {
			if (required) {
				throw getError("entity selector");
			}
			else {
				return null;
			}
		}
		
		try {
			return new EntitySelector(arg);
		}
		catch (IllegalArgumentException e) {
			throw getCustomError("Invalid entity in selector");
		}
	}
	
	public BlockSelector readBlockSelector(boolean required) throws CommandException {
		String arg = findPattern(wordPattern);
		if (arg == null) {
			if (required) {
				throw getError("block selector");
			}
			else {
				return null;
			}
		}
		
		try {
			return new BlockSelector(arg);
		}
		catch (IllegalArgumentException e) {
			throw getCustomError(e.getMessage());
		}
	}
	
	public List<String> readFlags() {
		List<String> flags = new ArrayList<>();
		String flag = findPattern(flagPattern);
		while (flag != null) {
			flags.add(flag);
			flag = findPattern(flagPattern);
		}
		return flags;
	}
	
	private String findPattern(Pattern pattern) {
		Matcher matcher = pattern.matcher(args);
		matcher.region(position, args.length());
		if (matcher.find()) {
			position = matcher.end();
			return matcher.group(1);
		}
		else {
			return null;
		}
	}
	
	public CommandException getError(String expectedType) {
		if (position == 0) {
			return new CommandException(String.format("Expected %s at: %s<--[HERE]", expectedType, command.getName()));
		}
		else {
			return new CommandException(String.format("Expected %s at: %s %s<--[HERE]", expectedType, command.getName(), args.substring(0, position)));
		}
	}
	
	public CommandException getGenericError() {
		if (position == 0) {
			return new CommandException(String.format("Invalid or missing argument at: %s<--[HERE]", command.getName()));
		}
		else {
			return new CommandException(String.format("Invalid or missing argument at: %s %s<--[HERE]", command.getName(), args.substring(0, position)));
		}
	}
	
	public CommandException getCustomError(String message) {
		if (position == 0) {
			return new CommandException(String.format("%s: %s<--[HERE]", message, command.getName()));
		}
		else {
			return new CommandException(String.format("%s: %s %s<--[HERE]", message, command.getName(), args.substring(0, position)));
		}
	}
}
