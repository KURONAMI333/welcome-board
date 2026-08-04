package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.Constants;
import com.kuronami.welcomeboard.config.LayoutPreset;
import com.kuronami.welcomeboard.platform.Services;
import com.kuronami.welcomeboard.seen.SeenFileStore;
import com.kuronami.welcomeboard.seen.SeenStateEvaluator;
import com.kuronami.welcomeboard.trigger.DisplayTrigger;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Drives {@link DisplayTrigger} every client tick and turns its verdict into actual screen-opening
 * (DESIGN_COMPILE.md U5 — the differentiator both prior-art mods left as an open, unresolved
 * GitHub issue). Loader entry points call {@link #onWorldJoin()}/{@link #onWorldLeave()} once each
 * from their login/logout listeners and {@link #onClientTick()} once per client tick; everything
 * else — loading content files, checking seen-state, opening/queuing screens, persisting
 * seen-state — happens here so neither loader module duplicates the logic.
 *
 * <p>Static/stateless-holder style (matching this project's existing {@code Services}/{@code
 * Constants}) rather than an instance: exactly one of these is ever active per running client.
 */
public final class DisplayQueueController {

    private DisplayQueueController() {
    }

    /** -1 means "not currently in a world" (main menu, between worlds, etc). */
    private static int ticksSinceJoin = -1;

    /** true once this world-session has either shown everything pending or given up. */
    private static boolean sessionResolved = false;

    /**
     * true from the moment the first screen of the queue is opened. Distinct from {@code
     * mc.screen instanceof WelcomeBoardScreen}: once showing has started, a player clicking a URL
     * button pushes a {@code ConfirmLinkScreen} on top (see {@code WelcomeBoardScreen#openConfirmLink}),
     * which would otherwise look identical to "some other mod's blocking screen" to {@link
     * DisplayTrigger} and eventually fire a false {@code GIVE_UP} — the tick loop has no further
     * business running once we've already committed to showing.
     */
    private static boolean showingStarted = false;

    private static Deque<ContentFileLoader.LoadedContent> pendingQueue;
    private static Map<String, Map<String, Integer>> seenData;
    private static String scopeKey;

    /** Call once from the client login listener (both loaders: NeoForge {@code ClientPlayerNetworkEvent.LoggingIn}, Fabric {@code ClientPlayConnectionEvents.JOIN}). */
    public static void onWorldJoin() {
        ticksSinceJoin = 0;
        sessionResolved = false;
        showingStarted = false;
        pendingQueue = null;
        seenData = null;
        scopeKey = null;
    }

    /** Call once from the client logout/disconnect listener so a later join in the same client session starts clean. */
    public static void onWorldLeave() {
        ticksSinceJoin = -1;
        sessionResolved = false;
        showingStarted = false;
        pendingQueue = null;
        seenData = null;
        scopeKey = null;
    }

    /** Call once per client tick (both loaders). No-op outside of a world or once this session is resolved. */
    public static void onClientTick() {
        if (ticksSinceJoin < 0 || sessionResolved || showingStarted) {
            return;
        }
        if (!Services.CONFIG.enabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        DisplayTrigger.Decision decision = DisplayTrigger.decide(ticksSinceJoin, minecraft.screen != null);
        switch (decision) {
            case WAIT -> ticksSinceJoin++;
            case GIVE_UP -> {
                int waitedTotal = DisplayTrigger.ENTRY_DELAY_TICKS + DisplayTrigger.SCREEN_WAIT_LIMIT_TICKS;
                Constants.LOG.warn(
                        "Welcome Board: another screen stayed open for {} ticks after world join; giving up for this session. Will try again next time this world/server is joined.",
                        waitedTotal);
                sessionResolved = true;
            }
            case SHOW -> beginShowing(minecraft);
        }
    }

    private static void beginShowing(Minecraft minecraft) {
        showingStarted = true;
        scopeKey = ScopeKey.current();
        seenData = SeenFileStore.load(ContentFileLoader.seenFile());

        List<ContentFileLoader.LoadedContent> all = ContentFileLoader.loadAll();
        pendingQueue = new ArrayDeque<>();
        for (ContentFileLoader.LoadedContent loaded : all) {
            if (SeenStateEvaluator.shouldShow(seenData, scopeKey, loaded.id(), loaded.content().revision())) {
                pendingQueue.add(loaded);
            }
        }

        showNextOrFinish(minecraft);
    }

    private static void showNextOrFinish(Minecraft minecraft) {
        if (pendingQueue == null || pendingQueue.isEmpty()) {
            sessionResolved = true;
            return;
        }

        ContentFileLoader.LoadedContent next = pendingQueue.poll();
        LayoutPreset preset = Services.CONFIG.layoutPreset();
        minecraft.setScreen(new WelcomeBoardScreen(next.content(), preset, () -> onScreenClosed(next)));
    }

    private static void onScreenClosed(ContentFileLoader.LoadedContent shown) {
        seenData = SeenStateEvaluator.markSeen(seenData, scopeKey, shown.id(), shown.content().revision());
        try {
            SeenFileStore.save(ContentFileLoader.seenFile(), seenData);
        } catch (IOException e) {
            // Never crash the game over a failed save (DESIGN_COMPILE.md's explicit instruction):
            // worst case, this content shows again next time.
            Constants.LOG.warn("Welcome Board: failed to save seen-state to '{}'; this content may show again next time: {}",
                    ContentFileLoader.seenFile(), e.toString());
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (pendingQueue != null && !pendingQueue.isEmpty()) {
            showNextOrFinish(minecraft);
        } else {
            sessionResolved = true;
            minecraft.setScreen(null);
        }
    }
}
