package com.kuronami.welcomeboard.seen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin file I/O for {@code config/welcome_board/seen.json}. Deliberately separated from
 * {@link SeenStateEvaluator} so the show/hide decision logic stays a pure, filesystem-free
 * function. Calc layer: no Minecraft types; the caller (loader side) resolves the actual
 * gamedir-relative {@link Path}.
 */
public final class SeenFileStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Map<String, Map<String, Integer>>>() {
    }.getType();

    private SeenFileStore() {
    }

    /**
     * Loads the seen-state map from {@code file}. Never throws: a missing, unreadable, or
     * corrupted file simply yields an empty map (equivalent to "nothing seen yet") rather
     * than crashing content display.
     */
    public static Map<String, Map<String, Integer>> load(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return new LinkedHashMap<>();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Map<String, Map<String, Integer>> data = GSON.fromJson(json, DATA_TYPE);
            return data != null ? data : new LinkedHashMap<>();
        } catch (IOException | JsonParseException e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * Writes the seen-state map to {@code file}, creating parent directories as needed.
     */
    public static void save(Path file, Map<String, Map<String, Integer>> data) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, GSON.toJson(data, DATA_TYPE), StandardCharsets.UTF_8);
    }
}
