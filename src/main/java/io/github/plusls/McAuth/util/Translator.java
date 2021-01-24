package io.github.plusls.McAuth.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

public class Translator {
    private static Config config;
    private static final Path configPath = Paths.get(FabricLoader.getInstance().getConfigDir().toString(), "mc_auth_mod.json");
    private static Map<String, String> translationMap;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String tr(String key) {
        return translationMap.getOrDefault(key, key);
    }

    public static void reloadLanguage() {
        if (!Files.exists(configPath)) {
            config = new Config();
            try {
                Files.createFile(configPath);
                BufferedWriter bfw = Files.newBufferedWriter(configPath);
                bfw.write(GSON.toJson(config));
                bfw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                BufferedReader bfr = Files.newBufferedReader(configPath);
                config = GSON.fromJson(bfr, Config.class);
                bfr.close();
            } catch (IOException e) {
                config = new Config();
                e.printStackTrace();
            }
        }
        try {
            InputStream inputStream = Translator.class.getClassLoader().getResourceAsStream(
                    String.format("assets/mc_auth_mod/lang/%s.json", config.language_code));
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            String jsonString = IOUtils.toString(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8);
            translationMap = GSON.fromJson(jsonString, type);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static class Config {

        private final String language_code;

        public Config(String language_code) {
            this.language_code = language_code;
        }

        public Config() {
            this("en_us");
        }

        public String getLanguageCode() {
            return language_code;
        }
    }
}
