package utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Properties properties = new Properties();

    static {
        loadProperties("src/test/resources/config.properties");
    }

    private static void loadProperties(String path) {
        try (InputStream is = new FileInputStream(path)) {
            properties.load(is);
        } catch (Exception e) {
            System.out.println("⚠ Could not load config.properties from " + path + ": " + e.getMessage());
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key, "");
    }

    public static String getProperty(String key, String defaultVal) {
        return properties.getProperty(key, defaultVal);
    }
}
