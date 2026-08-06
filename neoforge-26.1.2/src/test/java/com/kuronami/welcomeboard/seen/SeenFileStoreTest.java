package com.kuronami.welcomeboard.seen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeenFileStoreTest {

    @Test
    void loadingMissingFileReturnsEmptyMap(@TempDir Path tempDir) {
        Path file = tempDir.resolve("seen.json");
        Map<String, Map<String, Integer>> data = assertDoesNotThrow(() -> SeenFileStore.load(file));
        assertTrue(data.isEmpty());
    }

    @Test
    void saveThenLoadRoundTrips(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("nested/seen.json");
        Map<String, Map<String, Integer>> data = SeenStateEvaluator.markSeen(Map.of(), "world/MyWorld", "welcome", 3);

        SeenFileStore.save(file, data);
        Map<String, Map<String, Integer>> loaded = SeenFileStore.load(file);

        assertEquals(3, loaded.get("world/MyWorld").get("welcome"));
    }

    @Test
    void corruptedFileLoadsAsEmptyMapWithoutThrowing(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("seen.json");
        Files.writeString(file, "{ not valid json", StandardCharsets.UTF_8);

        Map<String, Map<String, Integer>> data = assertDoesNotThrow(() -> SeenFileStore.load(file));
        assertTrue(data.isEmpty());
    }
}
