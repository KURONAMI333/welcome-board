package com.kuronami.welcomeboard;

import com.kuronami.welcomeboard.platform.NeoForgeConfigHelper;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Constants.MOD_ID)
public class WelcomeBoard {

    public WelcomeBoard(IEventBus eventBus, ModContainer modContainer) {

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        modContainer.registerConfig(ModConfig.Type.CLIENT, NeoForgeConfigHelper.SPEC);
        if (FMLEnvironment.dist.isClient()) {
            // registerExtensionPoint touches client-only GUI classes; guard it so a dedicated
            // server never even tries to resolve ConfigurationScreen (NEW_MOD_GUIDE in-game
            // config convention).
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        // Use NeoForge to bootstrap the Common mod.
        CommonClass.init();
    }
}