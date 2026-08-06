package com.kuronami.welcomeboard.content;

/**
 * Resolved, defaulted representation of the optional {@code image} block in a content file.
 * {@code id} is an opaque resource-location style string (e.g. {@code "welcome_board:textures/gui/logo.png"});
 * resolving it to an actual texture is the renderer's job, not this layer's.
 */
public record WelcomeImage(
        String id,
        Anchor anchor,
        int width,
        int height,
        int offsetX,
        int offsetY
) {
}
