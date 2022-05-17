package com.github.phylogeny.boundtotems.blockentity;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf.BindingState;
import com.github.phylogeny.boundtotems.init.BlockEntitiesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityTotemShelfBinding extends BlockEntityTotemShelf {
    private static final int TICKS_MAX = 80;
    private int tickCounter;
    private Boolean cooling;

    public BlockEntityTotemShelfBinding(BlockPos pos, BlockState state) {
        super(BlockEntitiesMod.TOTEM_SHELF_BINDING.get(), pos, state);
    }

    public float getBindingPercentage() {
        float delayTicks = cooling ? 5F : 10F;
        float percentage = (tickCounter - delayTicks) / (TICKS_MAX - delayTicks);
        return Mth.clamp(cooling ? 1 - percentage : percentage, 0, 1);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, BlockEntityTotemShelfBinding shelf) {
        shelf.tick();
    }

    public void tick() {
        tickCounter++;
    }

    @Override
    public BlockState getBlockState() {
        try {
            return super.getBlockState();
        } finally {
            if (cooling == null)
                cooling = super.getBlockState().getValue(BlockTotemShelf.BINDING_STATE) == BindingState.COOLING;
        }
    }
}