package com.github.phylogeny.boundtotems.init;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.crafting.RecipeTotemBound;
import com.github.phylogeny.boundtotems.crafting.RecipeTotemBoundTeleporting;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RecipesMod
{
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPES = new DeferredRegister<>(ForgeRegistries.RECIPE_SERIALIZERS, BoundTotems.MOD_ID);

    public static final RegistryObject<RecipeTotemBound.Serializer> BOUND_TOTEM = RECIPES.register("bound_totem_copy", RecipeTotemBound.Serializer::new);
    public static final RegistryObject<RecipeTotemBoundTeleporting.Serializer> BOUND_TOTEM_TELEPORTING = RECIPES.register("bound_totem_teleporting", RecipeTotemBoundTeleporting.Serializer::new);
}
