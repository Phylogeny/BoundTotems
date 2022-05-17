package com.github.phylogeny.boundtotems.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Hashtable;
import java.util.Set;

public interface IShelfPositions {
    Hashtable<ResourceLocation, Set<BlockPos>> getPositions();
    void setPositions(Hashtable<ResourceLocation, Set<BlockPos>> positions);
}