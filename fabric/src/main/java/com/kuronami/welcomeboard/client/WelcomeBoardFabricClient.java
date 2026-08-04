package com.kuronami.welcomeboard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Wires {@link DisplayQueueController} into Fabric's client lifecycle events (DESIGN_COMPILE.md
 * U5). Registered as this project's "client" entrypoint (see {@code fabric.mod.json}) rather than
 * the main entrypoint so it is never even loaded on a dedicated server.
 */
public final class WelcomeBoardFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DisplayQueueController.onWorldJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DisplayQueueController.onWorldLeave());
        ClientTickEvents.END_CLIENT_TICK.register(client -> DisplayQueueController.onClientTick());
    }
}
