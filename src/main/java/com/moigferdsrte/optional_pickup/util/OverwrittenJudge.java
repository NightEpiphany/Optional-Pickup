package com.moigferdsrte.optional_pickup.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.Method;
import java.util.List;

public class OverwrittenJudge {
    @Contract(pure = true)
    public static boolean isHoverTextOverridden(Class<?> subclass) {
        try {
            Method parentMethod = Item.class.getDeclaredMethod("appendHoverText", ItemStack.class, Item.TooltipContext.class, List.class, TooltipFlag.class);
            Method childMethod = subclass.getMethod("appendHoverText", ItemStack.class, Item.TooltipContext.class, List.class, TooltipFlag.class);

            return !childMethod.getDeclaringClass().equals(parentMethod.getDeclaringClass());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
