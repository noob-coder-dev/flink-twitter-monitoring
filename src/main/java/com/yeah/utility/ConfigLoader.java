package com.yeah.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConfigLoader {
    private static ConfigLoader instance;
    private final Properties properties;

    private ConfigLoader() throws IOException {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Unable to find application.properties");
            }
            properties.load(input);
        }
    }

    public static synchronized ConfigLoader getInstance() throws IOException {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public List<String> getListProperty(String key) {
        String value = properties.getProperty(key);
        return Arrays.asList(value.split(","));
    }
}