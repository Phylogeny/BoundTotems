package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.PositionsTotemShelf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ItemCarvingKnife extends AxeItem {
    public ItemCarvingKnife(Properties properties) {
        super(Tiers.STONE, 7F, -3.2F, properties.stacksTo(1));
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
        PositionsTotemShelf positions = BlockTotemShelf.getTotemShelfPositions(state, world, pos);
        return positions == null || positions.getNextStage() == null || positions.getNextStage() > 6;
    }
}