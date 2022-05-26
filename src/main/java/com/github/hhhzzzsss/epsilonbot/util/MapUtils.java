package com.github.hhhzzzsss.epsilonbot.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Data;
import lombok.Getter;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MapUtils {
	
	@Data
	public static class MapColor {
		private final String block;
		private final Color[] colors; 
	}
	
	@Getter private static MapColor[] colors;
	
	static {
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("mapColors.json");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		JsonArray colorJsonArray = JsonParser.parseReader(reader).getAsJsonArray();
		
		colors = new MapColor[colorJsonArray.size()];
		for (int i = 0; i < colorJsonArray.size(); i++) {
			JsonObject mapColorObj = colorJsonArray.get(i).getAsJsonObject();
			
			String block = mapColorObj.get("block").getAsString();
			JsonArray shades = mapColorObj.get("colors").getAsJsonArray();
			MapColor mapColor = new MapColor(
					block,
					new Color[] {
							jsonArrayToColor(shades.get(0).getAsJsonArray()),
							jsonArrayToColor(shades.get(1).getAsJsonArray()),
							jsonArrayToColor(shades.get(2).getAsJsonArray()),
							jsonArrayToColor(shades.get(3).getAsJsonArray()),
					}
			);
			
			colors[i] = mapColor;
		}
	}
	
	private static Color jsonArrayToColor(JsonArray arr) {
		return new Color(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
	}
}
