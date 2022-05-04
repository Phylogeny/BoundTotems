package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.PositionsTotemShelf;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemTier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemCarvingKnife extends AxeItem
{
    public ItemCarvingKnife(Properties properties)
    {
        super(ItemTier.STONE, 7F, -3.2F, properties.stacksTo(1));
    }

    @Override
    public boolean canAttackBlock(BlockState state, World world, BlockPos pos, PlayerEntity player)
    {
        PositionsTotemShelf positions = BlockTotemShelf.getTotemShelfPositions(state, world, pos);
        return positions == null || positions.getNextStage() == null || positions.getNextStage() > 6;
    }
}