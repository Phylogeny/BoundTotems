package com.github.phylogeny.boundtotems.init;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.block.ShelfDropRemovalModifier;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class LootModifiersMod
{
    public static final DeferredRegister<GlobalLootModifierSerializer<?>> LOOT_MODIFIERS = new DeferredRegister<>(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS, BoundTotems.MOD_ID);

    public static final RegistryObject<ShelfDropRemovalModifier.Serializer> REMOVE_SHELF_DROP = LOOT_MODIFIERS.register("remove_shelf_drop", ShelfDropRemovalModifier.Serializer::new);
}
