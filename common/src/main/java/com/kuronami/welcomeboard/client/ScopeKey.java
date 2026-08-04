package com.kuronami.welcomeboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Resolves the opaque {@code scopeKey} string {@link com.kuronami.welcomeboard.seen.SeenStateEvaluator}
 * needs to keep "seen" state from leaking across servers/worlds (DESIGN_COMPILE.md §2/§5, item 5 —
 * the regression this design responds to: nvb-uy's Welcome Screen tracks "seen" for the whole
 * client instance, so the same modpack on a second server never shows the screen again).
 *
 * <p>Needs actual Minecraft types (the world folder name / server address), so it cannot live in
 * the {@code seen} calc-layer package (DESIGN_COMPILE.md §7 — this is one of the three places the
 * client-only surface is deliberately touched). {@link com.kuronami.welcomeboard.seen.SeenStateEvaluator}
 * only ever receives the result of this class as an opaque string; it never generates one itself.
 */
public final class ScopeKey {

    private ScopeKey() {
    }

    /**
     * @return {@code "singleplayer:<world folder name>"} for a local/LAN world, or
     *         {@code "multiplayer:<server address>"} for a remote server. Falls back to a fixed
     *         string if neither is available (e.g. called outside of any world — should not
     *         normally happen, but must never throw).
     */
    public static String current() {
        Minecraft mc = Minecraft.getInstance();

        IntegratedServer singleplayerServer = mc.getSingleplayerServer();
        if (singleplayerServer != null) {
            String worldFolder = singleplayerServer.getWorldPath(LevelResource.ROOT).getFileName().toString();
            return "singleplayer:" + worldFolder;
        }

        ServerData currentServer = mc.getCurrentServer();
        if (currentServer != null && currentServer.ip != null) {
            return "multiplayer:" + currentServer.ip;
        }

        return "unknown";
    }
}
