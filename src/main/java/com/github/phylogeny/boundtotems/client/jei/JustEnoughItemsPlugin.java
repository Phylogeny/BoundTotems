package com.github.phylogeny.boundtotems.client.jei;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.item.ItemTotemShelf;
import com.github.phylogeny.boundtotems.util.LangUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.block.Blocks;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.RegistryObject;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JustEnoughItemsPlugin implements IModPlugin
{
    private static final String ITEM_INFO_LANG_KEY = LangUtil.getKey(ModIds.JEI_ID, "item_info");

    @Override
    public ResourceLocation getPluginUid()
    {
        return new ResourceLocation(BoundTotems.MOD_ID, ModIds.JEI_ID);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        ItemStack stackBloodyKnife = new ItemStack(ItemsMod.RITUAL_DAGGER.get());
        NBTUtil.setBoundEntity(stackBloodyKnife, MathHelper.getRandomUUID(), I18n.format(LangUtil.join(InWorldRecipeCategory.LANG_KEY_PREFIX, "entity_name")));
        CompoundNBT nbtBloody = stackBloodyKnife.getTag();
        assert nbtBloody != null;
        CompoundNBT nbtBound = nbtBloody.copy();
        NBTUtil.bindKnife(nbtBound);
        ItemStack stackBoundKnife = stackBloodyKnife.copy();
        stackBloodyKnife.setTag(nbtBloody);
        stackBoundKnife.setTag(nbtBound);
        ItemStack stackTotemBound = new ItemStack(ItemsMod.BOUND_TOTEM.get());
        ItemStack stackTotemBoundTeleporting = new ItemStack(ItemsMod.BOUND_TOTEM_TELEPORTING.get());
        stackTotemBound.setTag(nbtBloody);
        stackTotemBoundTeleporting.setTag(nbtBloody);
        ItemStack stackPlanks = new ItemStack(ItemsMod.PLANK.get(), 4);

        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        List<InWorldRecipe> recipes = new ArrayList<>();
        ItemTotemShelf shelf = ItemsMod.TOTEM_SHELF_ITEM.get();
        recipes.add(new InWorldRecipe("bloody_dagger", guiHelper, 1, ItemsMod.RITUAL_DAGGER.get()).setOutputs(stackBloodyKnife));
        recipes.add(new InWorldRecipe("bind_totem", guiHelper, 2, stackBloodyKnife, stackBoundKnife).setAdditionalInputs(Items.TOTEM_OF_UNDYING).setOutputs(stackTotemBound));
        recipes.add(new InWorldRecipe("totem_shelf_frame", guiHelper, 9, ItemsMod.CARVING_KNIFE.get()).setAdditionalInputs(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG).setOutputs(stackPlanks));
        recipes.add(new InWorldRecipe("totem_shelf", guiHelper, 5, stackPlanks).setAdditionalInputs(shelf).setOutputs(shelf));
        recipes.add(new InWorldRecipe("bind_shelf", guiHelper, 4, stackBloodyKnife, stackBoundKnife).setAdditionalInputs(shelf).setOutputs(stackBoundKnife).setLangParameters(Config.SERVER.maxDistanceToShelf.get()));
        recipes.add(new InWorldRecipe("shelf_put_take_totems", guiHelper, 6, stackTotemBound, stackTotemBoundTeleporting).setAdditionalInputs(shelf).setOutputs(stackTotemBound, stackTotemBoundTeleporting));
        registration.addRecipes(recipes, InWorldRecipeCategory.UID);

        String allowedTotemLocations = I18n.format(LangUtil.join(ITEM_INFO_LANG_KEY, Config.SERVER.inventorySearch.get().name().toLowerCase()));
        addItemInfo(registration, ItemsMod.RITUAL_DAGGER);
        addItemInfo(registration, ItemsMod.BOUND_TOTEM, allowedTotemLocations);
        addItemInfo(registration, ItemsMod.BOUND_TOTEM_TELEPORTING);
        addItemInfo(registration, ItemsMod.CARVING_KNIFE);
        addItemInfo(registration, ItemsMod.PLANK);
        addItemInfo(registration, ItemsMod.TOTEM_SHELF_ITEM, allowedTotemLocations);
    }

    private static <I extends Item> void addItemInfo(IRecipeRegistration registration, RegistryObject<I> item, Object... parameters)
    {
        registration.addIngredientInfo(new ItemStack(item.get()), VanillaTypes.ITEM, I18n.format(LangUtil.join(ITEM_INFO_LANG_KEY, item.get().getRegistryName().getPath()), parameters));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
    {
        registration.addRecipeCatalyst(new ItemStack(Blocks.CRAFTING_TABLE), InWorldRecipeCategory.UID);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration)
    {
        registration.addRecipeCategories(new InWorldRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }
}