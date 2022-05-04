package com.github.phylogeny.boundtotems.init;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelf;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelfBinding;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.function.Supplier;

public class TileEntitiesMod {
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, BoundTotems.MOD_ID);

    public static final RegistryObject<TileEntityType<TileEntityTotemShelf>> TOTEM_SHELF = register("totem_shelf_tile", TileEntityTotemShelf::new, BlocksMod.TOTEM_SHELF);
    public static final RegistryObject<TileEntityType<TileEntityTotemShelfBinding>> TOTEM_SHELF_BINDING = register("totem_shelf_binding_tile", TileEntityTotemShelfBinding::new, BlocksMod.TOTEM_SHELF);

    private static <E extends TileEntity, B extends Block> RegistryObject<TileEntityType<E>> register(String name, Supplier<E> factory, RegistryObject<B>... blocks) {
        return TILE_ENTITIES.register(name, () -> TileEntityType.Builder.of(factory, Arrays.stream(blocks).map(RegistryObject::get).toArray(Block[]::new)).build(null));
    }
}