package com.kuronami.welcomeboard.trigger;

/**
 * Pure decision logic for the "when is it safe to show the welcome board" gate (DESIGN_COMPILE.md
 * U5). This is the calc layer for the display trigger: no Minecraft types, so the state
 * transitions can be exercised directly by JUnit even though the thing that actually drives ticks
 * and opens a {@code Screen} lives in the client layer.
 *
 * <p>The two non-negotiable numbers this encodes (DESIGN_COMPILE.md §5): wait {@link
 * #ENTRY_DELAY_TICKS} after world join for other mods' first-join screens (Origins etc.) to
 * appear, then if one is still open, wait up to {@link #SCREEN_WAIT_LIMIT_TICKS} more for it to
 * close before giving up. Giving up must never be silent (prior-art's failure mode — see
 * DESIGN_COMPILE.md §2): the caller is expected to log when {@link Decision#GIVE_UP} is returned.
 */
public final class DisplayTrigger {

    /** Ticks to wait after world join before even checking whether it's safe to show anything. */
    public static final int ENTRY_DELAY_TICKS = 20;

    /** Ticks to wait for a blocking screen to close, measured from the end of the entry delay. */
    public static final int SCREEN_WAIT_LIMIT_TICKS = 60;

    /** What the caller should do this tick. */
    public enum Decision {
        /** Keep waiting; call {@link #decide} again next tick with an incremented counter. */
        WAIT,
        /** Safe to show now: no other screen is in the way. */
        SHOW,
        /** A blocking screen never closed within the budget; stop trying this session and log it. */
        GIVE_UP
    }

    private DisplayTrigger() {
    }

    /**
     * @param ticksSinceJoin  ticks elapsed since the client joined the world (0 on the join tick
     *                        itself); negative values are treated the same as 0 (still waiting).
     * @param blockingScreenOpen whether some other, non-welcome-board {@code Screen} is currently
     *                        open (i.e. {@code Minecraft.getInstance().screen != null})
     * @return the action the caller should take this tick
     */
    public static Decision decide(int ticksSinceJoin, boolean blockingScreenOpen) {
        int elapsed = Math.max(0, ticksSinceJoin);

        if (elapsed < ENTRY_DELAY_TICKS) {
            return Decision.WAIT;
        }

        if (!blockingScreenOpen) {
            return Decision.SHOW;
        }

        int waitedForScreen = elapsed - ENTRY_DELAY_TICKS;
        return waitedForScreen >= SCREEN_WAIT_LIMIT_TICKS ? Decision.GIVE_UP : Decision.WAIT;
    }
}
