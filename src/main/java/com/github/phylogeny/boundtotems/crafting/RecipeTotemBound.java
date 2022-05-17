package com.github.phylogeny.boundtotems.crafting;

import com.github.phylogeny.boundtotems.item.ItemBoundTotem;
import com.github.phylogeny.boundtotems.item.ItemBoundTotemTeleporting;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class RecipeTotemBound extends ShapelessRecipe {
    public RecipeTotemBound(ResourceLocation id, String group, ItemStack recipeOutput, NonNullList<Ingredient> recipeItems) {
        super(id, group, recipeOutput, recipeItems);
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        return transferBoundTotemNBT(inv, super.assemble(inv), totem -> totem instanceof ItemBoundTotemTeleporting ? 2 : 1);
    }

    public static ItemStack transferBoundTotemNBT(CraftingContainer inv, ItemStack result, Function<Item, Integer> copySourceInstance) {
        if (!result.isEmpty()) {
            applyToBoundTotem(inv, copySourceInstance.apply(result.getItem()), (index, stack) -> result.setTag(stack.getTag() != null ? stack.getTag().copy() : null));
            if (result.getItem() instanceof ItemBoundTotemTeleporting)
                applyToBoundTotem(inv, 1, (index, stack) -> NBTUtil.copyBoundLocation(stack, result));
        }
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remainder = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        applyToBoundTotem(inv, 1, (index, stack) -> remainder.set(index, stack.copy()));
        inv.setChanged();
        return remainder;
    }

    private static void applyToBoundTotem(CraftingContainer inv, int instance, BiConsumer<Integer, ItemStack> operation) {
        ItemStack stack;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            stack = inv.getItem(i);
            if (stack.getItem() instanceof ItemBoundTotem && --instance == 0) {
                operation.accept(i, stack);
                return;
            }
        }
    }

    public static class Serializer extends ShapelessRecipe.Serializer {
        @Override
        public ShapelessRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ShapelessRecipe recipe = super.fromJson(recipeId, json);
            return new RecipeTotemBound(recipe.getId(), recipe.getGroup(), recipe.getResultItem(), recipe.getIngredients());
        }
    }
}