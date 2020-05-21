package com.github.phylogeny.boundtotems.util;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.capability.IShelfPositions;
import com.github.phylogeny.boundtotems.capability.ShelfPositionsProvider;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class CapabilityUtil
{
    public static IShelfPositions getShelfPositions(LivingEntity entity)
    {
        return entity.getCapability(ShelfPositionsProvider.CAPABILITY).orElseThrow(BoundTotems.EMPTY_OPTIONAL_EXP);
    }

    public static IItemHandler getInventory(TileEntityTotemShelf tileEntity)
    {
        return tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseThrow(BoundTotems.EMPTY_OPTIONAL_EXP);
    }
}
