package com.github.phylogeny.boundtotems.capability;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.Hashtable;
import java.util.Set;

public interface IShelfPositions
{
    Hashtable<DimensionType, Set<BlockPos>> getPositions();
    void setPositions(Hashtable<DimensionType, Set<BlockPos>> positions);
}
