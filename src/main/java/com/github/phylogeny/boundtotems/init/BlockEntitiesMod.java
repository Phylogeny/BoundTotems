package com.github.phylogeny.boundtotems.init;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelf;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelfBinding;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

public class BlockEntitiesMod {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, BoundTotems.MOD_ID);

    public static final RegistryObject<BlockEntityType<BlockEntityTotemShelf>> TOTEM_SHELF = register("totem_shelf_tile", BlockEntityTotemShelf::new, BlocksMod.TOTEM_SHELF);
    public static final RegistryObject<BlockEntityType<BlockEntityTotemShelfBinding>> TOTEM_SHELF_BINDING = register("totem_shelf_binding_tile", BlockEntityTotemShelfBinding::new, BlocksMod.TOTEM_SHELF);

    private static <E extends BlockEntity, B extends Block> RegistryObject<BlockEntityType<E>> register(String name, BlockEntityType.BlockEntitySupplier<E> factory, RegistryObject<B>... blocks) {
        return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(factory, Arrays.stream(blocks).map(RegistryObject::get).toArray(Block[]::new)).build(null));
    }
}