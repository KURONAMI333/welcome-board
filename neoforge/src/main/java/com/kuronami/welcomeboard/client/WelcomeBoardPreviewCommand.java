package com.kuronami.welcomeboard.client;

import com.kuronami.welcomeboard.Constants;
import com.kuronami.welcomeboard.config.LayoutPreset;
import com.kuronami.welcomeboard.content.ContentParser;
import com.kuronami.welcomeboard.content.ParseResult;
import com.kuronami.welcomeboard.content.WelcomeContent;
import com.kuronami.welcomeboard.platform.Services;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * {@code /welcomeboard preview [card|banner|full]} — opens the welcome-board screen on demand so a
 * pack author can check their {@code config/welcome_board/welcome.json} without waiting for the
 * once-per-session trigger.
 *
 * <p>Deliberately bypasses both {@link com.kuronami.welcomeboard.seen.SeenFileStore} (never read or
 * written here — this is a look, not an acknowledgement) and the persisted {@code layout_preset}
 * config value (the optional preset argument overrides only this one invocation; falls back to
 * {@link Services#CONFIG}'s current value when omitted). Re-parses {@code welcome.json} fresh on
 * every call — via {@link ContentParser} directly rather than {@link ContentFileLoader#loadAll()},
 * because {@code loadAll()} only logs {@link ParseResult#warnings()} and this command's whole point
 * is putting those warnings in the pack author's chat instead.
 *
 * <p>Registered on NeoForge's game event bus (like {@link WelcomeBoardNeoForgeClientEvents},
 * {@code RegisterClientCommandsEvent} fires on {@code NeoForge.EVENT_BUS}, not the mod bus).
 *
 * <p>Only usable once in a world: NeoForge's {@code ClientCommandHandler} (re)builds the client
 * command dispatcher from the {@code ClientPlayerNetworkEvent.LoggingIn} listener, so this command
 * does not exist yet at the title screen.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class WelcomeBoardPreviewCommand {

    private static final String CONTENT_FILE_NAME = "welcome.json";

    private WelcomeBoardPreviewCommand() {
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("welcomeboard")
                        .then(Commands.literal("preview")
                                .executes(ctx -> preview(ctx.getSource(), null))
                                .then(Commands.literal("card").executes(ctx -> preview(ctx.getSource(), LayoutPreset.CARD)))
                                .then(Commands.literal("banner").executes(ctx -> preview(ctx.getSource(), LayoutPreset.BANNER)))
                                .then(Commands.literal("full").executes(ctx -> preview(ctx.getSource(), LayoutPreset.FULL)))));
    }

    private static int preview(CommandSourceStack source, LayoutPreset presetOverride) {
        Path file = ContentFileLoader.configDir().resolve(CONTENT_FILE_NAME);

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (NoSuchFileException e) {
            source.sendFailure(Component.literal("Welcome Board: no content file at '" + file + "'."));
            return 0;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Welcome Board: could not read '" + file + "': "
                    + e.getClass().getSimpleName() + (e.getMessage() != null ? " (" + e.getMessage() + ")" : "")));
            return 0;
        }

        ParseResult parsed = ContentParser.parse(bytes);
        for (String warning : parsed.warnings()) {
            source.sendSuccess(() -> Component.literal("Welcome Board: " + warning), false);
        }

        WelcomeContent content = parsed.content();
        LayoutPreset preset = presetOverride != null ? presetOverride : Services.CONFIG.layoutPreset();

        // Deferred via tell(), not execute(): the ChatScreen that dispatched this command calls
        // setScreen(null) right after handleChatInput() returns (see ChatScreen#keyPressed), on the
        // same call stack as this method — reached from GLFW input polling, not from inside a
        // queued task. BlockableEventLoop#execute() only queues when isSameThread() is false or a
        // task is already running (ReentrantBlockableEventLoop#runningTask()); neither holds here,
        // so execute() would run this inline and ChatScreen's setScreen(null) would still win.
        // tell() always enqueues onto pendingRunnables regardless, so it runs on the next
        // runAllTasks() pass (start of the next runTick()) — strictly after ChatScreen closes.
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.tell(() -> minecraft.setScreen(new WelcomeBoardScreen(content, preset, null)));

        return 1;
    }
}
