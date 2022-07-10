package com.github.hhhzzzsss.epsilonbot.command;

import lombok.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@NoArgsConstructor
public class CommandList {
	private HashMap<String, Command> commandMap = new HashMap<>();
	private ArrayList<Command> commandList = new ArrayList<>();
	
	public void add(Command command) {
		commandList.add(command);
		commandMap.put(command.getName().toLowerCase(), command);
		for (String alias : command.getAliases()) {
			if (!commandMap.containsKey(alias)) {
				commandMap.put(alias.toLowerCase(), command);
			}
		}
	}
	
	public Command get(String name) {
		return commandMap.get(name);
	}
	
	public boolean contains(String name) {
		return commandMap.containsKey(name);
	}
	
	public List<Command> getCommands() {
		return commandList;
	}

	public static File permissionsFile = new File("command-permissions.yml");
	public void loadPermissionsFromFile() {
		if (permissionsFile.exists()) {
			try {
				Yaml yaml = new Yaml();
				Map<String, Integer> obj = yaml.load(new FileInputStream(permissionsFile));
				for (Map.Entry<String, Integer> entry : obj.entrySet()) {
					if (commandMap.containsKey(entry.getKey())) {
						commandMap.get(entry.getKey()).setPermission(entry.getValue());
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	public void savePermissionsToFile() {
		try {
			Map<String, Object> obj = new HashMap<>();
			for (Command command : commandMap.values()) {
				obj.put(command.getName(), command.getPermission());
			}
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			Yaml yaml = new Yaml(options);
			FileWriter writer = new FileWriter(permissionsFile);
			yaml.dump(obj, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
