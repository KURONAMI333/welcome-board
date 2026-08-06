package com.kuronami.welcomeboard.content;

import java.util.List;

/**
 * Outcome of {@link ContentParser#parse}. {@code content} is never null: even a completely
 * broken input yields a fully-defaulted, renderable {@link WelcomeContent}. Anything that had
 * to be defaulted, dropped, or truncated is recorded in {@code warnings} for the caller to log.
 */
public record ParseResult(WelcomeContent content, List<String> warnings) {

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
