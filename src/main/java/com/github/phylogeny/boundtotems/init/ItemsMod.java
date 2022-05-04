package com.github.phylogeny.boundtotems.init;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.item.*;
import com.google.common.base.Suppliers;
import net.minecraft.block.Block;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.*;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemsMod
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BoundTotems.MOD_ID);

    public static final RegistryObject<ItemRitualDagger> RITUAL_DAGGER = registerItem(ItemRitualDagger.NAME, ItemRitualDagger::new);
    public static final RegistryObject<ItemBoundTotem> BOUND_TOTEM = registerItem("bound_totem", ItemBoundTotem::new);
    public static final RegistryObject<ItemBoundTotemTeleporting> BOUND_TOTEM_TELEPORTING = registerItem("bound_totem_teleporting", ItemBoundTotemTeleporting::new);
    public static final RegistryObject<ItemCarvingKnife> CARVING_KNIFE = registerItem("carving_knife", ItemCarvingKnife::new);
    public static final RegistryObject<Item> PLANK = registerItem("plank", Item::new);
    public static final RegistryObject<BlockItem> TOTEM_SHELF_ITEM = registerBlockItem(BlockTotemShelf.NAME, BlocksMod.TOTEM_SHELF, BlockItem::new);
    public static final RegistryObject<ItemBoundCompass> BOUND_COMPASS = registerItem("bound_compass", ItemBoundCompass::new);

    private static final Supplier<Map<Item, Item>> IN_WORLD_ITEM_CONVERSIONS = Suppliers.memoize(() ->
    {
        Map<Item, Item> map = new HashMap<>();
        map.put(Items.TOTEM_OF_UNDYING, BOUND_TOTEM.get());
        map.put(Items.COMPASS, BOUND_COMPASS.get());
        return map;
    });

    public static ItemStack getBoundItem(ItemEntity entityTotem)
    {
        if (entityTotem != null)
        {
            Item item = IN_WORLD_ITEM_CONVERSIONS.get().get(entityTotem.getItem().getItem());
            if (item != null)
                return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    private static <I extends Item> RegistryObject<I> registerItem(String name, Function<Item.Properties, I> function)
    {
        return ITEMS.register(name, () -> function.apply(getProperties()));
    }

    private static <I extends BlockItem, B extends Block> RegistryObject<I> registerBlockItem(String name, Supplier<B> block, BiFunction<Block, Item.Properties, I> function)
    {
        return ITEMS.register(name, () -> function.apply(block.get(), getProperties()));
    }

    public static Item.Properties getProperties()
    {
        return new Item.Properties().tab(CREATIVE_TAB);
    }

    private static final ItemGroup CREATIVE_TAB = new ItemGroup(BoundTotems.MOD_ID)
    {
        @Override
        public ItemStack makeIcon()
        {
            return new ItemStack(BOUND_TOTEM.get());
        }
    };
}