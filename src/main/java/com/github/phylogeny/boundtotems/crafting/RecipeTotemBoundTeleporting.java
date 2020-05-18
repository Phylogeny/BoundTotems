package com.github.phylogeny.boundtotems.crafting;

import com.github.phylogeny.boundtotems.item.ItemBoundTotem;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.UUID;

public class RecipeTotemBoundTeleporting extends ShapedRecipe
{
    public RecipeTotemBoundTeleporting(ResourceLocation id, String group, int recipeWidth,
            int recipeHeight, NonNullList<Ingredient> recipeItems, ItemStack recipeOutput)
    {
        super(id, group, recipeWidth, recipeHeight, recipeItems, recipeOutput);
    }

    @Override
    public ItemStack getCraftingResult(CraftingInventory inv)
    {
        return RecipeTotemBound.transferBoundTotemNBT(inv, super.getCraftingResult(inv), totem -> 1);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world)
    {
        boolean matches = super.matches(inv, world);
        if (matches)
        {
            // Ensure that if the recipe contains multiple bound totems, they all have the same bound entity
            ItemStack stack;
            UUID entityId = null;
            for (int i = 0; i < inv.getSizeInventory(); i++)
            {
                stack = inv.getStackInSlot(i);
                if (stack.getItem() instanceof ItemBoundTotem)
                {
                    if (entityId == null)
                        entityId = NBTUtil.getBoundEntityId(stack);
                    else if (!entityId.equals(NBTUtil.getBoundEntityId(stack)))
                    {
                        matches = false;
                        break;
                    }
                }
            }
        }
        return matches;
    }

    public static class Serializer extends ShapedRecipe.Serializer
    {
        @Override
        public ShapedRecipe read(ResourceLocation recipeId, JsonObject json)
        {
            ShapedRecipe recipe = super.read(recipeId, json);
            return new RecipeTotemBoundTeleporting(recipe.getId(), recipe.getGroup(), recipe.getRecipeWidth(),
                    recipe.getRecipeHeight(), recipe.getIngredients(), recipe.getRecipeOutput());
        }
    }
}