package com.github.phylogeny.boundtotems.block;

import com.github.phylogeny.boundtotems.item.ItemCarvingKnife;
import net.minecraft.block.BlockState;
import net.minecraft.block.LogBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

import javax.annotation.Nullable;

public class BlockStrippedOakLog extends LogBlock
{
    public static final SoundType CARVE_WOOD = new SoundType(SoundType.WOOD.getVolume() * 2, SoundType.WOOD.getPitch() * 2, SoundType.WOOD.getBreakSound(),
            SoundType.WOOD.getStepSound(), SoundType.WOOD.getPlaceSound(), SoundEvents.ITEM_AXE_STRIP, SoundType.WOOD.getFallSound());

    public BlockStrippedOakLog(Properties properties)
    {
        super(MaterialColor.WOOD, properties);
    }

    @Override
    public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity)
    {
        return getSoundType(state, entity);
    }

    public static SoundType getSoundType(BlockState state, Entity entity)
    {
        if (entity instanceof LivingEntity
                && ((LivingEntity) entity).getItemStackFromSlot(EquipmentSlotType.MAINHAND).getItem() instanceof ItemCarvingKnife)
            return CARVE_WOOD;

        return state.getSoundType();
    }
}