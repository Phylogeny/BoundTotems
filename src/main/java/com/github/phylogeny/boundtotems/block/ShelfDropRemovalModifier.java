package com.github.phylogeny.boundtotems.block;

import com.github.phylogeny.boundtotems.item.ItemTotemShelf;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.ILootCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ShelfDropRemovalModifier extends LootModifier
{
    private static boolean shouldRemove;

    public ShelfDropRemovalModifier(ILootCondition[] conditions)
    {
        super(conditions);
    }

    public static void setRemoval(boolean shouldRemove)
    {
        ShelfDropRemovalModifier.shouldRemove = shouldRemove;
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context)
    {
        if (!shouldRemove)
            return generatedLoot;

        List<ItemStack> drops = new ArrayList<>();
        generatedLoot.forEach(drop ->
        {
            if (!(drop.getItem() instanceof ItemTotemShelf))
                drops.add(drop);
        });
        return drops;
    }

    public static class Serializer extends GlobalLootModifierSerializer<ShelfDropRemovalModifier>
    {
        @Override
        public ShelfDropRemovalModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions)
        {
            return new ShelfDropRemovalModifier(conditions);
        }
    }
}
