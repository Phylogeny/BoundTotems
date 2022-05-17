package com.github.phylogeny.boundtotems.client.jei;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.init.ItemsMod;
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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fmllegacy.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JeiPlugin
public class JustEnoughItemsPlugin implements IModPlugin {
    private static final String ITEM_INFO_LANG_KEY = LangUtil.getKey(ModIds.JEI_ID, "item_info");

    @Override
    public ResourceLocation getPluginUid() {
        return BoundTotems.getResourceLoc(ModIds.JEI_ID);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ItemStack stackBloodyKnife = new ItemStack(ItemsMod.RITUAL_DAGGER.get());
        NBTUtil.setBoundEntity(stackBloodyKnife, Mth.createInsecureUUID(), I18n.get(LangUtil.join(InWorldRecipeCategory.LANG_KEY_PREFIX, "entity_name")));
        CompoundTag nbtBloody = stackBloodyKnife.getTag();
        assert nbtBloody != null;
        CompoundTag nbtBound = nbtBloody.copy();
        NBTUtil.bindKnife(nbtBound);
        ItemStack stackBoundKnife = stackBloodyKnife.copy();
        stackBloodyKnife.setTag(nbtBloody);
        stackBoundKnife.setTag(nbtBound);
        ItemStack stackTotemBound = new ItemStack(ItemsMod.BOUND_TOTEM.get());
        ItemStack stackTotemBoundTeleporting = new ItemStack(ItemsMod.BOUND_TOTEM_TELEPORTING.get());
        ItemStack stackCompassBound = new ItemStack(ItemsMod.BOUND_COMPASS.get());
        stackTotemBound.setTag(nbtBloody);
        stackTotemBoundTeleporting.setTag(nbtBloody);
        stackCompassBound.setTag(nbtBloody);
        ItemStack stackPlanks = new ItemStack(ItemsMod.PLANK.get(), 4);

        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        List<InWorldRecipe> recipes = new ArrayList<>();
        Item shelf = ItemsMod.TOTEM_SHELF_ITEM.get();
        recipes.add(new InWorldRecipe("bloody_dagger", guiHelper, 1, ItemsMod.RITUAL_DAGGER.get()).setOutputs(stackBloodyKnife));
        recipes.add(new InWorldRecipe("bind_totem", guiHelper, 2, stackBloodyKnife, stackBoundKnife).setAdditionalInputs(Items.TOTEM_OF_UNDYING).setOutputs(stackTotemBound));
        recipes.add(new InWorldRecipe("totem_shelf_frame", guiHelper, 9, ItemsMod.CARVING_KNIFE.get()).setAdditionalInputs(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG).setOutputs(stackPlanks));
        recipes.add(new InWorldRecipe("totem_shelf", guiHelper, 5, stackPlanks).setAdditionalInputs(shelf).setOutputs(shelf));
        recipes.add(new InWorldRecipe("bind_shelf", guiHelper, 4, stackBloodyKnife, stackBoundKnife).setAdditionalInputs(shelf).setOutputs(stackBoundKnife).setLangParameters(Config.SERVER.maxDistanceToShelf.get(), Config.SERVER.maxBoundShelves.get()));
        recipes.add(new InWorldRecipe("shelf_put_take_totems", guiHelper, 6, stackTotemBound, stackTotemBoundTeleporting).setAdditionalInputs(shelf).setOutputs(stackTotemBound, stackTotemBoundTeleporting));
        recipes.add(new InWorldRecipe("bind_compass", guiHelper, 2, stackBloodyKnife, stackBoundKnife).setAdditionalInputs(Items.COMPASS).setOutputs(stackCompassBound));
        registration.addRecipes(recipes, InWorldRecipeCategory.UID);

        String allowedTotemLocations = I18n.get(LangUtil.join(ITEM_INFO_LANG_KEY, Config.SERVER.inventorySearch.get().name().toLowerCase()));
        addItemInfo(registration, ItemsMod.RITUAL_DAGGER);
        addItemInfo(registration, ItemsMod.BOUND_TOTEM, allowedTotemLocations);
        addItemInfo(registration, ItemsMod.BOUND_TOTEM_TELEPORTING);
        addItemInfo(registration, ItemsMod.CARVING_KNIFE);
        addItemInfo(registration, ItemsMod.PLANK);
        addItemInfo(registration, ItemsMod.TOTEM_SHELF_ITEM, allowedTotemLocations);
        addItemInfo(registration, ItemsMod.BOUND_COMPASS);
    }

    private static <I extends Item> void addItemInfo(IRecipeRegistration registration, RegistryObject<I> item, Object... parameters) {
        registration.addIngredientInfo(new ItemStack(item.get()), VanillaTypes.ITEM, new TranslatableComponent(LangUtil.join(ITEM_INFO_LANG_KEY, Objects.requireNonNull(item.get().getRegistryName()).getPath()), parameters));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Blocks.CRAFTING_TABLE), InWorldRecipeCategory.UID);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new InWorldRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }
}