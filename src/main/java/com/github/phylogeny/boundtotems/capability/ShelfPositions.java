package com.github.phylogeny.boundtotems.capability;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.Hashtable;
import java.util.Set;

public class ShelfPositions implements IShelfPositions
{
    private Hashtable<DimensionType, Set<BlockPos>> positions;

    public ShelfPositions()
    {
        positions = new Hashtable<>();
    }

    @Override
    public Hashtable<DimensionType, Set<BlockPos>> getPositions()
    {
        return positions;
    }

    @Override
    public void setPositions(Hashtable<DimensionType, Set<BlockPos>> positions)
    {
        this.positions = positions;
    }
}
