package com.github.phylogeny.boundtotems.capability;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.Hashtable;
import java.util.Set;

public interface IShelfPositions {
    Hashtable<ResourceLocation, Set<BlockPos>> getPositions();

    void setPositions(Hashtable<ResourceLocation, Set<BlockPos>> positions);
}