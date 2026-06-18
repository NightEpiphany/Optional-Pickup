package com.moigferdsrte.optional_pickup.compact;

import com.moigferdsrte.optional_pickup.config.OptionalPickupConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;

public class ModMenuEntryPoint implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return s -> AutoConfigClient.getConfigScreen(OptionalPickupConfig.class, s).get();
    }
}
