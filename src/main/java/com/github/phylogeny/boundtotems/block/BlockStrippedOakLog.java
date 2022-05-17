package com.github.phylogeny.boundtotems.block;

import com.github.phylogeny.boundtotems.item.ItemCarvingKnife;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class BlockStrippedOakLog extends RotatedPillarBlock {
    public static final SoundType CARVE_WOOD = new SoundType(SoundType.WOOD.getVolume() * 2, SoundType.WOOD.getPitch() * 2, SoundType.WOOD.getBreakSound(),
            SoundType.WOOD.getStepSound(), SoundType.WOOD.getPlaceSound(), SoundEvents.AXE_STRIP, SoundType.WOOD.getFallSound());

    public BlockStrippedOakLog(Properties properties) {
        super(properties);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, @Nullable Entity entity) {
        return getSoundType(state, entity);
    }

    public static SoundType getSoundType(BlockState state, Entity entity) {
        if (entity instanceof LivingEntity livingEntity
                && livingEntity.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof ItemCarvingKnife)
            return CARVE_WOOD;

        return state.getSoundType();
    }
}