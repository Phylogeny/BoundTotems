package com.github.phylogeny.boundtotems.tileentity;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf.BindingState;
import com.github.phylogeny.boundtotems.init.TileEntitiesMod;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.math.MathHelper;

public class TileEntityTotemShelfBinding extends TileEntityTotemShelf implements ITickableTileEntity
{
    private static final int TICKS_MAX = 80;
    private int tickCounter;
    private Boolean cooling;

    public TileEntityTotemShelfBinding()
    {
        super(TileEntitiesMod.TOTEM_SHELF_BINDING.get());
    }

    public float getBindingPercentage()
    {
        float delayTicks = cooling ? 5F : 10F;
        float percentage = (tickCounter - delayTicks) / (TICKS_MAX - delayTicks);
        return MathHelper.clamp(cooling ? 1 - percentage : percentage, 0, 1);
    }

    @Override
    public void tick()
    {
        tickCounter++;
    }

    @Override
    public BlockState getBlockState()
    {
        try
        {
            return super.getBlockState();
        }
        finally
        {
            if (cooling == null)
                cooling = super.getBlockState().get(BlockTotemShelf.BINDING_STATE) == BindingState.COOLING;
        }
    }

    @Override
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        cooling = null;
    }
}