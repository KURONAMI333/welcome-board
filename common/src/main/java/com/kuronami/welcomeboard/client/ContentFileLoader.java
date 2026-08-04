package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.Constants;
import com.kuronami.welcomeboard.content.ContentParser;
import com.kuronami.welcomeboard.content.ParseResult;
import com.kuronami.welcomeboard.content.WelcomeContent;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Reads {@code config/welcome_board/<id>.json} content files from disk and feeds their raw bytes
 * to {@link ContentParser} (DESIGN_COMPILE.md U1's contract: pass raw bytes, never a decoded
 * String, so its UTF-8 validation actually runs). Needs the game directory, hence client layer and
 * not the calc layer.
 *
 * <p>Multiple content files coexist by filename-derived id (DESIGN_COMPILE.md §2 item 3, the
 * regression this responds to: prior art's {@code clearData()} could not have more than one file
 * active at once). {@code seen.json} is reserved for {@link com.kuronami.welcomeboard.seen.SeenFileStore}
 * and is always excluded from the content listing.
 */
public final class ContentFileLoader {

    private static final String SEEN_FILE_NAME = "seen.json";
    private static final String JSON_EXTENSION = ".json";

    private ContentFileLoader() {
    }

    /** One parsed content file, paired with the id derived from its filename (sans {@code .json}). */
    public record LoadedContent(String id, WelcomeContent content) {
    }

    /** {@code <gamedir>/config/welcome_board/} — also where {@link com.kuronami.welcomeboard.seen.SeenFileStore} keeps {@code seen.json}. */
    public static Path configDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(Constants.MOD_ID);
    }

    public static Path seenFile() {
        return configDir().resolve(SEEN_FILE_NAME);
    }

    /**
     * Loads every content file in {@link #configDir()} except {@code seen.json}, in filename
     * order. Never throws: a missing directory yields an empty list, an unreadable file is
     * skipped with a warning, and a malformed file (per {@link ContentParser}'s own contract)
     * still yields a fully-defaulted, renderable {@link WelcomeContent} rather than being
     * dropped — DESIGN_COMPILE.md §2's non-negotiable that a broken content file must never
     * result in silence.
     */
    public static List<LoadedContent> loadAll() {
        Path dir = configDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        List<Path> jsonFiles;
        try (Stream<Path> paths = Files.list(dir)) {
            jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(JSON_EXTENSION))
                    .filter(p -> !p.getFileName().toString().equalsIgnoreCase(SEEN_FILE_NAME))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            Constants.LOG.warn("Welcome Board: failed to list content directory '{}'; showing nothing this session: {}",
                    dir, e.toString());
            return List.of();
        }

        List<LoadedContent> result = new ArrayList<>(jsonFiles.size());
        for (Path file : jsonFiles) {
            String id = stripJsonExtension(file.getFileName().toString());
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file);
            } catch (IOException e) {
                Constants.LOG.warn("Welcome Board: failed to read content file '{}'; skipping: {}",
                        file.getFileName(), e.toString());
                continue;
            }
            ParseResult parsed = ContentParser.parse(bytes);
            for (String warning : parsed.warnings()) {
                Constants.LOG.warn("Welcome Board: content file '{}': {}", file.getFileName(), warning);
            }
            result.add(new LoadedContent(id, parsed.content()));
        }
        return result;
    }

    private static String stripJsonExtension(String filename) {
        return filename.substring(0, filename.length() - JSON_EXTENSION.length());
    }
}
