package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.Constants;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Wires {@link DisplayQueueController} into NeoForge's client event bus (DESIGN_COMPILE.md U5).
 *
 * <p>{@code value = Dist.CLIENT} is required, not optional: without it a dedicated server would
 * try to load this class too and fail resolving the client-only event types it references
 * (PLAYBOOK_CLIENT_RENDER.md's Phase 1 absolute-principle explanation of the same requirement on
 * {@code @EventBusSubscriber}-annotated classes).
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class WelcomeBoardNeoForgeClientEvents {

    private WelcomeBoardNeoForgeClientEvents() {
    }

    @SubscribeEvent
    static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        DisplayQueueController.onWorldJoin();
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DisplayQueueController.onWorldLeave();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        DisplayQueueController.onClientTick();
    }
}
