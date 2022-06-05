package com.github.hhhzzzsss.epsilonbot.command;

import lombok.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class CommandList {
	private HashMap<String, Command> commands = new HashMap<>();
	
	public void add(Command command) {
		commands.put(command.getName().toLowerCase(), command);
	}
	
	public Command get(String name) {
		return commands.get(name);
	}
	
	public boolean contains(String name) {
		return commands.containsKey(name);
	}
	
	public Collection<Command> getCommands() {
		return commands.values();
	}

	public static File permissionsFile = new File("command-permissions.yml");
	public void loadPermissionsFromFile() {
		if (permissionsFile.exists()) {
			try {
				Yaml yaml = new Yaml();
				Map<String, Integer> obj = yaml.load(new FileInputStream(permissionsFile));
				for (Map.Entry<String, Integer> entry : obj.entrySet()) {
					if (commands.containsKey(entry.getKey())) {
						commands.get(entry.getKey()).setPermission(entry.getValue());
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
			for (Command command : commands.values()) {
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
