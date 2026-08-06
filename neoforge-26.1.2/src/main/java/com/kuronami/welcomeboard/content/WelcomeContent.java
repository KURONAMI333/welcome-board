package com.kuronami.welcomeboard.content;

import java.util.List;

/**
 * Fully-defaulted content for one welcome-board file. A {@link ContentParser} result is
 * always renderable as-is, regardless of how damaged the source JSON was.
 */
public record WelcomeContent(
        int revision,
        String title,
        List<String> body,
        WelcomeImage image, // nullable: the image block is optional
        List<WelcomeButton> buttons,
        String closeText
) {
}
