package com.github.phylogeny.boundtotems.crafting;

import com.github.phylogeny.boundtotems.item.ItemBoundTotem;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class RecipeTotemBoundTeleporting extends ShapedRecipe {
    public RecipeTotemBoundTeleporting(ResourceLocation id, String group, int recipeWidth,
                                       int recipeHeight, NonNullList<Ingredient> recipeItems, ItemStack recipeOutput) {
        super(id, group, recipeWidth, recipeHeight, recipeItems, recipeOutput);
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        return RecipeTotemBound.transferBoundTotemNBT(inv, super.assemble(inv), totem -> 1);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        boolean matches = super.matches(inv, world);
        if (matches) {
            // Ensure that if the recipe contains multiple bound totems, they all have the same bound entity
            ItemStack stack;
            UUID entityId = null;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                stack = inv.getItem(i);
                if (stack.getItem() instanceof ItemBoundTotem) {
                    if (entityId == null)
                        entityId = NBTUtil.getBoundEntityId(stack);
                    else if (!entityId.equals(NBTUtil.getBoundEntityId(stack))) {
                        matches = false;
                        break;
                    }
                }
            }
        }
        return matches;
    }

    public static class Serializer extends ShapedRecipe.Serializer {
        @Override
        public ShapedRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ShapedRecipe recipe = super.fromJson(recipeId, json);
            return new RecipeTotemBoundTeleporting(recipe.getId(), recipe.getGroup(), recipe.getRecipeWidth(),
                    recipe.getRecipeHeight(), recipe.getIngredients(), recipe.getResultItem());
        }
    }
}