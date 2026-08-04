package com.kuronami.welcomeboard.content;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentParserTest {

    private static final String FULL_JSON = """
            {
              "revision": 3,
              "title": "Welcome to My Pack",
              "body": ["First line.", "Second line."],
              "image": { "id": "welcome_board:textures/gui/logo.png", "anchor": "top", "width": 128, "height": 64 },
              "buttons": [
                { "text": "Wiki", "url": "https://example.com/wiki", "anchor": "bottom_left" },
                { "text": "Discord", "url": "https://discord.gg/xxxx", "anchor": "bottom_right" }
              ],
              "close_text": "Let's go"
            }
            """;

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // ---- positive ----

    @Test
    void fullJsonReadsAllFields() {
        ParseResult result = ContentParser.parse(bytes(FULL_JSON));

        assertFalse(result.hasWarnings(), "a fully valid file should not produce warnings: " + result.warnings());
        WelcomeContent content = result.content();
        assertEquals(3, content.revision());
        assertEquals("Welcome to My Pack", content.title());
        assertEquals(List.of("First line.", "Second line."), content.body());
        assertEquals("Let's go", content.closeText());

        assertNotNull(content.image());
        assertEquals("welcome_board:textures/gui/logo.png", content.image().id());
        assertEquals(Anchor.TOP, content.image().anchor());
        assertEquals(128, content.image().width());
        assertEquals(64, content.image().height());

        assertEquals(2, content.buttons().size());
        assertEquals("Wiki", content.buttons().get(0).text());
        assertEquals("https://example.com/wiki", content.buttons().get(0).url());
        assertEquals(Anchor.BOTTOM_LEFT, content.buttons().get(0).anchor());
        assertEquals(Anchor.BOTTOM_RIGHT, content.buttons().get(1).anchor());
    }

    // ---- negative: never throw, always fall back, always warn ----

    @Test
    void missingTitleFallsBackAndWarns() {
        String json = """
                { "revision": 1, "body": ["hi"] }
                """;
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(json)));

        assertEquals(ContentParser.DEFAULT_TITLE, result.content().title());
        assertTrue(result.hasWarnings());
    }

    @Test
    void bodyNotArrayFallsBackAndWarns() {
        String json = """
                { "title": "Hi", "body": "not an array" }
                """;
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(json)));

        assertEquals(List.of(), result.content().body());
        assertTrue(result.hasWarnings());
    }

    @Test
    void buttonsNotArrayFallsBackAndWarns() {
        String json = """
                { "title": "Hi", "buttons": { "text": "oops" } }
                """;
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(json)));

        assertEquals(List.of(), result.content().buttons());
        assertTrue(result.hasWarnings());
    }

    @Test
    void fileOver64KbFallsBackAndWarns() {
        // pad well past the 64KB cap with a huge body line so the raw byte length trips the limit
        String hugeJson = "{ \"title\": \"Hi\", \"body\": [\"" + "x".repeat(ContentParser.MAX_FILE_BYTES + 10) + "\"] }";
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(hugeJson)));

        assertEquals(ContentParser.DEFAULT_TITLE, result.content().title());
        assertTrue(result.hasWarnings());
    }

    @Test
    void invalidUtf8FallsBackAndWarns() {
        // 0xC3 expects a continuation byte; 0x28 ('(') is not one -> malformed UTF-8 sequence.
        byte[] malformed = new byte[]{'{', (byte) 0xC3, 0x28, '}'};
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(malformed));

        assertEquals(ContentParser.DEFAULT_TITLE, result.content().title());
        assertTrue(result.hasWarnings());
    }

    @Test
    void javascriptUrlIsDroppedAndWarns() {
        String json = """
                { "title": "Hi", "buttons": [
                  { "text": "evil", "url": "javascript:alert(1)" },
                  { "text": "ok", "url": "https://example.com" }
                ] }
                """;
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(json)));

        assertEquals(1, result.content().buttons().size());
        assertEquals("ok", result.content().buttons().get(0).text());
        assertTrue(result.hasWarnings());
    }

    @Test
    void malformedJsonSyntaxFallsBackAndWarns() {
        byte[] garbage = bytes("{ this is not json at all :::");
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(garbage));

        assertEquals(ContentParser.DEFAULT_TITLE, result.content().title());
        assertTrue(result.hasWarnings());
    }

    @Test
    void emptyFileFallsBackAndWarns() {
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(new byte[0]));

        assertEquals(ContentParser.DEFAULT_TITLE, result.content().title());
        assertTrue(result.hasWarnings());
    }

    @Test
    void nonObjectRootFallsBackAndWarns() {
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes("[1, 2, 3]")));

        assertEquals(ContentParser.DEFAULT_TITLE, result.content().title());
        assertTrue(result.hasWarnings());
    }

    @Test
    void buttonLimitTruncatesExcessAndWarns() {
        StringBuilder buttons = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            if (i > 0) buttons.append(",");
            buttons.append("{ \"text\": \"b").append(i).append("\", \"url\": \"https://example.com/").append(i).append("\" }");
        }
        buttons.append("]");
        String json = "{ \"title\": \"Hi\", \"buttons\": " + buttons + " }";

        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(json)));

        assertEquals(ContentParser.MAX_BUTTONS, result.content().buttons().size());
        assertTrue(result.hasWarnings());
    }

    @Test
    void bodyLineLimitTruncatesExcessAndWarns() {
        StringBuilder body = new StringBuilder("[");
        for (int i = 0; i < 40; i++) {
            if (i > 0) body.append(",");
            body.append("\"line").append(i).append("\"");
        }
        body.append("]");
        String json = "{ \"title\": \"Hi\", \"body\": " + body + " }";

        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(json)));

        assertEquals(ContentParser.MAX_BODY_LINES, result.content().body().size());
        assertTrue(result.hasWarnings());
    }

    @Test
    void missingImageIdIsIgnoredAndWarns() {
        String json = """
                { "title": "Hi", "image": { "anchor": "top", "width": 10, "height": 10 } }
                """;
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(json)));

        assertNull(result.content().image());
        assertTrue(result.hasWarnings());
    }

    @Test
    void unrecognizedAnchorFallsBackToDefaultAndWarns() {
        String json = """
                { "title": "Hi", "buttons": [ { "text": "b", "url": "https://example.com", "anchor": "somewhere" } ] }
                """;
        ParseResult result = assertDoesNotThrow(() -> ContentParser.parse(bytes(json)));

        assertEquals(Anchor.DEFAULT, result.content().buttons().get(0).anchor());
        assertTrue(result.hasWarnings());
    }
}
