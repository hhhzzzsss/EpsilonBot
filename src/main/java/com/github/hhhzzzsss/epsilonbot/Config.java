package com.github.hhhzzzsss.epsilonbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Data
public class Config {
    static final File file = new File("config.yml");
    @Getter private static Config config;

    String host = "play.totalfreedom.me";
    int port = 25565;
    String username;
    String password;
    String authType = "microsoft";
    int buildSyncX;
    int buildSyncZ;
    int mapartX;
    int mapartZ;
    String commandPrefix = "`";
    String warpName = "epsilon_mapart";
    ArrayList<String> trusted;
    static {
        if (!file.exists()) {
            // creates config file from default-config.yml
            InputStream is = ClassLoader.getSystemClassLoader()
                .getResourceAsStream("default-config.yml");
            try {
                Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            IOUtils.closeQuietly(is);
        }
        Constructor constructor = new Constructor(Config.class);
        TypeDescription typeDescription = new TypeDescription(Config.class);
        typeDescription.addPropertyParameters("trusted", String.class);
        constructor.addTypeDescription(typeDescription);
        Yaml yaml = new Yaml(constructor);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            config = yaml.load(is);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    boolean mapartRatelimitEnabled;
    int mapartRatelimit;
}
