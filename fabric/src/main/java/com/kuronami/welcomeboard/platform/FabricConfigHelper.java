package com.kuronami.welcomeboard.platform;

import com.kuronami.welcomeboard.platform.services.IConfigHelper;

/**
 * Fabric implementation: fixed defaults, matching NeoForge's own defaults. Fabric has no
 * built-in in-game config screen mechanism (unlike NeoForge's {@code ConfigurationScreen}), and
 * this project's established convention for that gap (see mod-051's {@code FabricConfigHelper})
 * is to return sensible fixed values rather than build a bespoke config UI for one platform only.
 */
public class FabricConfigHelper implements IConfigHelper {

    @Override
    public boolean enabled() {
        return true;
    }
}
