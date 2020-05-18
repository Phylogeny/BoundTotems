package com.github.phylogeny.boundtotems.init;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.block.BlockStrippedOakLog;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;

public class BlocksMod
{
    public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, BoundTotems.MOD_ID);

    public static final RegistryObject<BlockTotemShelf> TOTEM_SHELF = register(BlockTotemShelf.NAME, BlockTotemShelf::new);
    public static final RegistryObject<BlockStrippedOakLog> STRIPPED_OAK_LOG = register("stripped_oak_log", BlockStrippedOakLog::new);

    private static <T extends Block> RegistryObject<T> register(String name, Function<Block.Properties, T> factory)
    {
        return register(name, factory, Material.WOOD, SoundType.WOOD, 2F);
    }

    private static <T extends Block> RegistryObject<T> register(String name, Function<Block.Properties, T> factory, Material material, SoundType sound, float hardnessAndResistance)
    {
        return BLOCKS.register(name, () -> factory.apply(Block.Properties.create(material).sound(sound).hardnessAndResistance(hardnessAndResistance)));
    }
}