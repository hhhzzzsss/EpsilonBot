package com.github.hhhzzzsss.epsilonbot.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Data;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class ItemUtils {
	
	@Data
	public static class ItemData {
		private final int id;
		private final String displayName;
		private final String name;
		private final int stackSize;

	}
	
	private static ItemData[] items;
	private static HashMap<String, ItemData> itemsByName = new HashMap<>();

	static {
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("items.json");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		JsonArray itemJsonArray = JsonParser.parseReader(reader).getAsJsonArray();
		
		items = new ItemData[itemJsonArray.size() + 1];
		for (JsonElement itemElem : itemJsonArray) {
			JsonObject itemObj = itemElem.getAsJsonObject();
			ItemData itemData = new ItemData(
					itemObj.get("id").getAsInt(),
					itemObj.get("displayName").getAsString(),
					itemObj.get("name").getAsString(),
					itemObj.get("stackSize").getAsInt());
			items[itemData.id] = itemData;
			itemsByName.put(itemData.name, itemData);
		}
	}
	
	public static ItemData getItem(int i) {
		return items[i];
	}
	
	public static ItemData getItemByName(String name) {
		return itemsByName.get(name);
	}
	
}
