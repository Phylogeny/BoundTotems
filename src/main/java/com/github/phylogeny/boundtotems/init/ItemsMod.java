package com.github.phylogeny.boundtotems.init;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.item.*;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ItemsMod
{
    public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, BoundTotems.MOD_ID);

    public static final RegistryObject<ItemRitualDagger> RITUAL_DAGGER = registerItem(ItemRitualDagger.NAME, ItemRitualDagger::new);
    public static final RegistryObject<ItemBoundTotem> BOUND_TOTEM = registerItem("bound_totem", ItemBoundTotem::new);
    public static final RegistryObject<ItemBoundTotemTeleporting> BOUND_TOTEM_TELEPORTING = registerItem("bound_totem_teleporting", ItemBoundTotemTeleporting::new);
    public static final RegistryObject<ItemCarvingKnife> CARVING_KNIFE = registerItem("carving_knife", ItemCarvingKnife::new);
    public static final RegistryObject<ItemPlank> PLANK = registerItem("plank", ItemPlank::new);
    public static final RegistryObject<ItemTotemShelf> TOTEM_SHELF_ITEM = registerBlockItem(BlockTotemShelf.NAME, BlocksMod.TOTEM_SHELF, ItemTotemShelf::new);

    private static <I extends Item> RegistryObject<I> registerItem(String name, Function<Item.Properties, I> function)
    {
        return ITEMS.register(name, () -> function.apply(getProperties()));
    }

    private static <I extends BlockItem, B extends Block> RegistryObject<I> registerBlockItem(String name, RegistryObject<B> block, BiFunction<Block, Item.Properties, I> function)
    {
        return ITEMS.register(name, () -> function.apply(block.get(), getProperties()));
    }

    public static Item.Properties getProperties()
    {
        return new Item.Properties().group(CREATIVE_TAB);
    }

    private static final ItemGroup CREATIVE_TAB = new ItemGroup(BoundTotems.MOD_ID)
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(BOUND_TOTEM.get());
        }
    };
}