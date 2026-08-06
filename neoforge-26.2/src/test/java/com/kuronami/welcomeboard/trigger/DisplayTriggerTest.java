package com.kuronami.welcomeboard.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kuronami.welcomeboard.trigger.DisplayTrigger.Decision;
import org.junit.jupiter.api.Test;

class DisplayTriggerTest {

    @Test
    void waitsDuringEntryDelayRegardlessOfScreenState() {
        assertEquals(Decision.WAIT, DisplayTrigger.decide(0, false));
        assertEquals(Decision.WAIT, DisplayTrigger.decide(19, false));
        assertEquals(Decision.WAIT, DisplayTrigger.decide(19, true));
    }

    @Test
    void showsAssoonAsEntryDelayElapsesWithNoBlockingScreen() {
        assertEquals(Decision.SHOW, DisplayTrigger.decide(20, false));
        assertEquals(Decision.SHOW, DisplayTrigger.decide(500, false));
    }

    @Test
    void waitsForABlockingScreenToClearWithinBudget() {
        assertEquals(Decision.WAIT, DisplayTrigger.decide(20, true));
        assertEquals(Decision.WAIT, DisplayTrigger.decide(20 + 59, true));
    }

    @Test
    void givesUpOnceTheScreenWaitBudgetIsExceeded() {
        assertEquals(Decision.GIVE_UP, DisplayTrigger.decide(20 + 60, true));
        assertEquals(Decision.GIVE_UP, DisplayTrigger.decide(20 + 200, true));
    }

    @Test
    void aClearedScreenAfterGiveUpThresholdStillShows() {
        // Regression guard: once the screen is no longer blocking, we show regardless of how
        // long we waited — GIVE_UP is only for a screen that is *still* open past the budget.
        assertEquals(Decision.SHOW, DisplayTrigger.decide(20 + 200, false));
    }

    @Test
    void negativeTicksSinceJoinAreTreatedAsNotStarted() {
        assertEquals(Decision.WAIT, DisplayTrigger.decide(-1, false));
    }
}
