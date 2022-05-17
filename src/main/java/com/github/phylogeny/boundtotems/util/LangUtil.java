package com.github.phylogeny.boundtotems.util;

import com.github.phylogeny.boundtotems.BoundTotems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class LangUtil {
    public static String join(String... elements) {
        return String.join(".", elements);
    }

    public static String getKey(String category, String... path) {
        return join(ArrayUtils.addAll(new String[]{category, BoundTotems.MOD_ID}, path));
    }

    public static TranslatableComponent getLocalizedText(String category, String name, Object... args) {
        return new TranslatableComponent(getKey(category, name), args);
    }

    public static TranslatableComponent getTooltip(String name, Object... args) {
        return getLocalizedText("tooltip", name, args);
    }

    public static void addTooltip(List<Component> tooltip, String name, Object... args) {
        tooltip.add(getTooltip(name, args));
    }

    public static void addTooltipWithFormattedSuffix(List<Component> tooltip, String name, String suffix, ChatFormatting... formatting) {
        tooltip.add(getTooltip(name).append(new TextComponent(suffix).withStyle(formatting)));
    }
}