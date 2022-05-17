package com.github.phylogeny.boundtotems.util;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.capability.IShelfPositions;
import com.github.phylogeny.boundtotems.capability.ShelfPositionsProvider;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class CapabilityUtil {
    public static IShelfPositions getShelfPositions(LivingEntity entity) {
        return entity.getCapability(ShelfPositionsProvider.CAPABILITY).orElseThrow(BoundTotems.EMPTY_OPTIONAL_EXP);
    }

    public static IItemHandler getInventory(BlockEntityTotemShelf blockEntity) {
        return blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseThrow(BoundTotems.EMPTY_OPTIONAL_EXP);
    }
}