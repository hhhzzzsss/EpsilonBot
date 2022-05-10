package com.github.hhhzzzsss.epsilonbot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {
	public static final Pattern ARG_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?(s|%)");
	public static final HashMap<String, String> LANGUAGE_MAP = new HashMap<>(); 
	static {
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("language.json");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		JsonObject languageJson = JsonParser.parseReader(reader).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : languageJson.entrySet()) {
			LANGUAGE_MAP.put(entry.getKey(), languageJson.get(entry.getKey()).getAsString());
		}
	}
	
	public static String getFullText(Component message) {
		StringBuilder sb = new StringBuilder();
		if (message instanceof TextComponent) {
			sb.append(getText((TextComponent) message));
		}
		else if (message instanceof TranslatableComponent) {
			sb.append(getText((TranslatableComponent) message));
		}
		else {
			return message.toString();
		}
		
		for (Component extra : message.children()) {
			sb.append(getFullText(extra));
		}
		return sb.toString();
	}
	
	public static String getText(TextComponent message) {
		String text = message.content();
		if (text == null) {
			return "";
		}
		else {
			return text;
		}
	}
	
	public static String getText(TranslatableComponent message) {
		String translate = LANGUAGE_MAP.get(message.key());
		if (translate == null) {
			return "";
		}
		Matcher matcher = ARG_PATTERN.matcher(translate);
		StringBuffer sb = new StringBuffer();
		
		int i = 0;
		while (matcher.find()) {
			if (matcher.group().equals("%%")) {
				matcher.appendReplacement(sb, "%");
			}
			else {
				String idxStr = matcher.group(1);
				int idx = idxStr == null ? i++ : Integer.parseInt(idxStr);
				if (idx < message.args().size()) {
					matcher.appendReplacement(sb, Matcher.quoteReplacement( getFullText(message.args().get(idx)) ));
				}
				else {
					matcher.appendReplacement(sb, "");
				}
			}
		}
		matcher.appendTail(sb);
		
		return sb.toString();
	}
	
	public static String escapeString(String string) {
		return string.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}
}
