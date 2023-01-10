package com.github.hhhzzzsss.epsilonbot.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ArgsParser {
	private final Command command;
	private final String args;
	@Getter private int position = 0;
	@Getter @Setter private FlagParser flagParser = null;
	
	public static final Pattern stringPattern = Pattern.compile("^\\s*(.+)", Pattern.DOTALL);
	public static final Pattern wordPattern = Pattern.compile("^\\s*(\\S+)");
	public static final Pattern shortFlagPattern = Pattern.compile("^\\s*-([^\\s0-9-]\\S*)");
	public static final Pattern longFlagPattern = Pattern.compile("^\\s*--([^\\s0-9-]\\S*)");
	public static final Pattern emptyPattern = Pattern.compile("^(\\s*)");
	
	public String readString(boolean required) throws CommandException {
		parseFlags();
		String arg = findPattern(stringPattern);
		if (arg == null && required) {
			throw getError("string");
		}
		
		return arg;
	}
	
	public String readWord(boolean required) throws CommandException {
		parseFlags();
		String arg = findPattern(wordPattern);
		if (arg == null && required) {
			throw getGenericError();
		}
		
		return arg;
	}
	
	public Integer readInt(boolean required) throws CommandException {
		parseFlags();
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
		parseFlags();
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
		parseFlags();
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

	public void end() throws CommandException {
		parseFlags();
		if (position < args.length() && !emptyPattern.matcher(args).region(position, args.length()).matches()) {
			throw getError("end of command");
		}
	}
	
	public void parseFlags() throws CommandException {
		if (flagParser != null) {
			boolean foundFlag = true;
			while (foundFlag) {
				foundFlag = false;
				String shortFlag = findPattern(shortFlagPattern);
				String longFlag = findPattern(longFlagPattern);
				if (shortFlag != null) {
					foundFlag = true;
					for (String c : shortFlag.split("")) {
						flagParser.parse(c);
					}
				}
				if (longFlag != null) {
					foundFlag = true;
					flagParser.parse(longFlag);
				}
			}
		}
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
