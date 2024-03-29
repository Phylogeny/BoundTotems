package com.github.phylogeny.boundtotems.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Hashtable;
import java.util.Set;

public class ShelfPositions implements IShelfPositions {
    private Hashtable<ResourceLocation, Set<BlockPos>> positions;

    public ShelfPositions() {
        positions = new Hashtable<>();
    }

    @Override
    public Hashtable<ResourceLocation, Set<BlockPos>> getPositions() {
        return positions;
    }

    @Override
    public void setPositions(Hashtable<ResourceLocation, Set<BlockPos>> positions) {
        this.positions = positions;
    }
}