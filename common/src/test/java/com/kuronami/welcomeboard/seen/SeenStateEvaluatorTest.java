package com.kuronami.welcomeboard.seen;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeenStateEvaluatorTest {

    // ---- positive ----

    @Test
    void firstTimeShowsContent() {
        Map<String, Map<String, Integer>> empty = Map.of();
        assertTrue(SeenStateEvaluator.shouldShow(empty, "world/MyWorld", "welcome", 1));
    }

    @Test
    void secondTimeHidesContent() {
        Map<String, Map<String, Integer>> data = SeenStateEvaluator.markSeen(Map.of(), "world/MyWorld", "welcome", 1);
        assertFalse(SeenStateEvaluator.shouldShow(data, "world/MyWorld", "welcome", 1));
    }

    @Test
    void revisionIncreaseShowsAgain() {
        Map<String, Map<String, Integer>> data = SeenStateEvaluator.markSeen(Map.of(), "world/MyWorld", "welcome", 1);
        assertTrue(SeenStateEvaluator.shouldShow(data, "world/MyWorld", "welcome", 2));
    }

    @Test
    void revisionEqualOrLowerStaysHidden() {
        Map<String, Map<String, Integer>> data = SeenStateEvaluator.markSeen(Map.of(), "world/MyWorld", "welcome", 5);
        assertFalse(SeenStateEvaluator.shouldShow(data, "world/MyWorld", "welcome", 5));
        assertFalse(SeenStateEvaluator.shouldShow(data, "world/MyWorld", "welcome", 3));
    }

    // ---- negative: the nvb-uy regression ----

    @Test
    void differentScopeKeyStillShows() {
        // seen on server A must not suppress the same content on server B.
        Map<String, Map<String, Integer>> data = SeenStateEvaluator.markSeen(Map.of(), "server-a.example.com", "welcome", 1);
        assertTrue(SeenStateEvaluator.shouldShow(data, "server-b.example.com", "welcome", 1));
    }

    @Test
    void differentContentIdWithinSameScopeStillShows() {
        Map<String, Map<String, Integer>> data = SeenStateEvaluator.markSeen(Map.of(), "world/MyWorld", "welcome", 1);
        assertTrue(SeenStateEvaluator.shouldShow(data, "world/MyWorld", "changelog", 1));
    }

    @Test
    void markSeenDoesNotMutateInputMap() {
        Map<String, Map<String, Integer>> original = Map.of();
        SeenStateEvaluator.markSeen(original, "world/MyWorld", "welcome", 1);
        // Map.of() is immutable; if markSeen tried to mutate it in place this would already have thrown.
        assertTrue(original.isEmpty());
    }
}
