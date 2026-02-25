package com.moigferdsrte.optional_pickup.config;
import com.moigferdsrte.optional_pickup.OptionalPickup;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
@Config(name = OptionalPickup.MOD_ID)
public class OptionalPickupConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int offsetX = 0;
    @ConfigEntry.BoundedDiscrete(min = -30, max = 60)
    @ConfigEntry.Gui.Tooltip
    public int offsetY = 0;
}
