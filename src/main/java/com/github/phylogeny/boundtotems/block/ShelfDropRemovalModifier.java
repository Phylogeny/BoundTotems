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
import java.util.stream.Collectors;

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
        return !shouldRemove ? generatedLoot :
                generatedLoot.stream().filter(stack -> !(stack.getItem() instanceof ItemTotemShelf)).collect(Collectors.toList());
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
