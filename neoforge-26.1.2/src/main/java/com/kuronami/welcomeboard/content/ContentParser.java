package com.kuronami.welcomeboard.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fault-tolerant parser for welcome-board content files ({@code config/welcome_board/<id>.json}).
 *
 * <p>Absolute rule: this class never throws for malformed input, no matter how damaged the
 * file is. Every field is defaulted independently; anything that had to be defaulted, dropped,
 * or truncated is recorded as a human-readable warning for the caller to log. This is the calc
 * layer — no Minecraft/NeoForge/Fabric types are used or imported here.</p>
 */
public final class ContentParser {

    public static final int MAX_FILE_BYTES = 64 * 1024;
    public static final int MAX_BUTTONS = 8;
    public static final int MAX_BODY_LINES = 32;

    public static final String DEFAULT_TITLE = "Welcome";
    public static final String DEFAULT_CLOSE_TEXT = "Close";

    private ContentParser() {
    }

    /**
     * Parses raw file bytes into a fully-defaulted {@link WelcomeContent}. Never throws.
     */
    public static ParseResult parse(byte[] rawBytes) {
        List<String> warnings = new ArrayList<>();

        if (rawBytes == null || rawBytes.length == 0) {
            warnings.add("content file is empty; using defaults");
            return new ParseResult(defaultContent(), warnings);
        }
        if (rawBytes.length > MAX_FILE_BYTES) {
            warnings.add("content file exceeds " + MAX_FILE_BYTES + " bytes (" + rawBytes.length
                    + "); ignoring file and using defaults");
            return new ParseResult(defaultContent(), warnings);
        }

        String json = decodeUtf8(rawBytes, warnings);
        if (json == null) {
            // decodeUtf8 already recorded the warning.
            return new ParseResult(defaultContent(), warnings);
        }

        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed == null || !parsed.isJsonObject()) {
                warnings.add("content file root is not a JSON object; using defaults");
                return new ParseResult(defaultContent(), warnings);
            }
            root = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            warnings.add("content file is not valid JSON; using defaults");
            return new ParseResult(defaultContent(), warnings);
        }

        int revision = readInt(root, "revision", 0, warnings);
        String title = readString(root, "title", DEFAULT_TITLE, warnings, true);
        List<String> body = readBody(root, warnings);
        WelcomeImage image = readImage(root, warnings);
        List<WelcomeButton> buttons = readButtons(root, warnings);
        String closeText = readString(root, "close_text", DEFAULT_CLOSE_TEXT, warnings, false);

        return new ParseResult(new WelcomeContent(revision, title, body, image, buttons, closeText), warnings);
    }

    private static WelcomeContent defaultContent() {
        return new WelcomeContent(0, DEFAULT_TITLE, List.of(), null, List.of(), DEFAULT_CLOSE_TEXT);
    }

    private static String decodeUtf8(byte[] rawBytes, List<String> warnings) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(rawBytes));
            return decoded.toString();
        } catch (CharacterCodingException e) {
            warnings.add("content file is not valid UTF-8; using defaults");
            return null;
        }
    }

    private static int readInt(JsonObject root, String key, int fallback, List<String> warnings) {
        JsonElement element = root.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
        } catch (RuntimeException ignored) {
            // fall through to warning below
        }
        warnings.add("'" + key + "' is not a valid integer; using default " + fallback);
        return fallback;
    }

    private static String readString(JsonObject root, String key, String fallback, List<String> warnings, boolean warnIfMissing) {
        JsonElement element = root.get(key);
        if (element == null || element.isJsonNull()) {
            if (warnIfMissing) {
                warnings.add("'" + key + "' is missing; using default \"" + fallback + "\"");
            }
            return fallback;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return element.getAsString();
        }
        warnings.add("'" + key + "' is not a string; using default \"" + fallback + "\"");
        return fallback;
    }

    private static List<String> readBody(JsonObject root, List<String> warnings) {
        JsonElement element = root.get("body");
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            warnings.add("'body' is not an array; using empty body");
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<String> lines = new ArrayList<>();
        int dropped = 0;
        for (JsonElement entry : array) {
            if (lines.size() >= MAX_BODY_LINES) {
                dropped++;
                continue;
            }
            if (entry != null && entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                lines.add(entry.getAsString());
            } else {
                dropped++;
            }
        }
        if (dropped > 0) {
            warnings.add("'body' had " + dropped + " invalid or excess line(s) beyond the "
                    + MAX_BODY_LINES + "-line limit; dropped");
        }
        return List.copyOf(lines);
    }

    private static List<WelcomeButton> readButtons(JsonObject root, List<String> warnings) {
        JsonElement element = root.get("buttons");
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            warnings.add("'buttons' is not an array; using no buttons");
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<WelcomeButton> buttons = new ArrayList<>();
        int droppedExcess = 0;
        for (JsonElement entry : array) {
            if (entry == null || !entry.isJsonObject()) {
                warnings.add("a 'buttons' entry is not an object; skipped");
                continue;
            }
            if (buttons.size() >= MAX_BUTTONS) {
                droppedExcess++;
                continue;
            }
            WelcomeButton button = readButton(entry.getAsJsonObject(), warnings);
            if (button != null) {
                buttons.add(button);
            }
        }
        if (droppedExcess > 0) {
            warnings.add("'buttons' exceeded the " + MAX_BUTTONS + "-button limit; "
                    + droppedExcess + " entry/entries dropped");
        }
        return List.copyOf(buttons);
    }

    private static WelcomeButton readButton(JsonObject buttonObject, List<String> warnings) {
        String text = readString(buttonObject, "text", "", warnings, false);
        String rawUrl = readString(buttonObject, "url", "", warnings, false);
        if (rawUrl.isEmpty() || !(rawUrl.startsWith("http://") || rawUrl.startsWith("https://"))) {
            warnings.add("a button URL is missing or not http(s); button dropped");
            return null;
        }
        Anchor anchor = readAnchor(buttonObject, warnings);
        int[] offset = readOffset(buttonObject, warnings);
        return new WelcomeButton(text, rawUrl, anchor, offset[0], offset[1]);
    }

    private static WelcomeImage readImage(JsonObject root, List<String> warnings) {
        JsonElement element = root.get("image");
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonObject()) {
            warnings.add("'image' is not an object; ignoring image");
            return null;
        }
        JsonObject imageObject = element.getAsJsonObject();
        String id = readString(imageObject, "id", "", warnings, false);
        if (id.isEmpty()) {
            warnings.add("'image.id' is missing; ignoring image");
            return null;
        }
        Anchor anchor = readAnchor(imageObject, warnings);
        int width = Math.max(0, readInt(imageObject, "width", 0, warnings));
        int height = Math.max(0, readInt(imageObject, "height", 0, warnings));
        int[] offset = readOffset(imageObject, warnings);
        return new WelcomeImage(id, anchor, width, height, offset[0], offset[1]);
    }

    private static Anchor readAnchor(JsonObject object, List<String> warnings) {
        JsonElement element = object.get("anchor");
        if (element == null || element.isJsonNull()) {
            return Anchor.DEFAULT;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            Anchor anchor = Anchor.fromString(element.getAsString());
            if (anchor != null) {
                return anchor;
            }
        }
        warnings.add("'anchor' value is not recognized; using default " + Anchor.DEFAULT);
        return Anchor.DEFAULT;
    }

    private static int[] readOffset(JsonObject object, List<String> warnings) {
        JsonElement element = object.get("offset");
        if (element == null || element.isJsonNull()) {
            return new int[]{0, 0};
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() == 2) {
                try {
                    return new int[]{array.get(0).getAsInt(), array.get(1).getAsInt()};
                } catch (RuntimeException ignored) {
                    // fall through to warning below
                }
            }
        }
        warnings.add("'offset' is not a 2-element number array; using [0, 0]");
        return new int[]{0, 0};
    }
}
