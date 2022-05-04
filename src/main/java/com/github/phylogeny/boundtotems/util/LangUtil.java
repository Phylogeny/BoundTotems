package com.github.phylogeny.boundtotems.util;

import com.github.phylogeny.boundtotems.BoundTotems;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class LangUtil
{
    public static String join(String... elements)
    {
        return String.join(".", elements);
    }

    public static String getKey(String category, String... path)
    {
        return join(ArrayUtils.addAll(new String[]{category, BoundTotems.MOD_ID}, path));
    }

    public static TranslationTextComponent getLocalizedText(String category, String name, Object... args)
    {
        return new TranslationTextComponent(getKey(category, name), args);
    }

    public static TranslationTextComponent getTooltip(String name, Object... args)
    {
        return getLocalizedText("tooltip", name, args);
    }

    public static void addTooltip(List<ITextComponent> tooltip, String name, Object... args)
    {
        tooltip.add(getTooltip(name, args));
    }

    public static void addTooltipWithFormattedSuffix(List<ITextComponent> tooltip, String name, String suffix, TextFormatting... formatting)
    {
        tooltip.add(getTooltip(name).append(new StringTextComponent(suffix).withStyle(formatting)));
    }
}
