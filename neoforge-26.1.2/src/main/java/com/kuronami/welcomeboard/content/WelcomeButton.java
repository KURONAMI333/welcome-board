package com.kuronami.welcomeboard.content;

/**
 * Resolved, defaulted representation of one entry in the {@code buttons} array.
 * {@code url} is guaranteed to be {@code http://} or {@code https://} by the time
 * a button reaches this record; anything else is dropped during parsing.
 */
public record WelcomeButton(
        String text,
        String url,
        Anchor anchor,
        int offsetX,
        int offsetY
) {
}
