package com.github.phylogeny.boundtotems.crafting;

import com.github.phylogeny.boundtotems.item.ItemBoundTotem;
import com.github.phylogeny.boundtotems.item.ItemBoundTotemTeleporting;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class RecipeTotemBound extends ShapelessRecipe
{
    public RecipeTotemBound(ResourceLocation id, String group, ItemStack recipeOutput, NonNullList<Ingredient> recipeItems)
    {
        super(id, group, recipeOutput, recipeItems);
    }

    @Override
    public ItemStack getCraftingResult(CraftingInventory inv)
    {
        return transferBoundTotemNBT(inv, super.getCraftingResult(inv), totem -> totem instanceof ItemBoundTotemTeleporting ? 2 : 1);
    }

    public static ItemStack transferBoundTotemNBT(CraftingInventory inv, ItemStack result, Function<Item, Integer> copySourceInstance)
    {
        if (!result.isEmpty())
        {
            applyToBoundTotem(inv, copySourceInstance.apply(result.getItem()), (index, stack) -> result.setTag(stack.getTag() != null ? stack.getTag().copy() : null));
            if (result.getItem() instanceof ItemBoundTotemTeleporting)
                applyToBoundTotem(inv, 1, (index, stack) -> NBTUtil.copyBoundLocation(stack, result));
        }
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv)
    {
        NonNullList<ItemStack> remainder = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        applyToBoundTotem(inv, 1, (index, stack) -> remainder.set(index, stack.copy()));
        inv.markDirty();
        return remainder;
    }

    private static void applyToBoundTotem(CraftingInventory inv, int instance, BiConsumer<Integer, ItemStack> operation)
    {
        ItemStack stack;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            stack = inv.getStackInSlot(i);
            if (stack.getItem() instanceof ItemBoundTotem && --instance == 0)
            {
                operation.accept(i, stack);
                return;
            }
        }
    }

    public static class Serializer extends ShapelessRecipe.Serializer
    {
        @Override
        public ShapelessRecipe read(ResourceLocation recipeId, JsonObject json)
        {
            ShapelessRecipe recipe = super.read(recipeId, json);
            return new RecipeTotemBound(recipe.getId(), recipe.getGroup(), recipe.getRecipeOutput(), recipe.getIngredients());
        }
    }
}