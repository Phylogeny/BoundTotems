package com.github.phylogeny.boundtotems.capability;

import com.github.phylogeny.boundtotems.BoundTotems;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ShelfPositionsProvider implements ICapabilitySerializable<INBT>
{
    @CapabilityInject(IShelfPositions.class)
    public static final Capability<IShelfPositions> CAPABILITY = null;

    private final LazyOptional<IShelfPositions> instance = LazyOptional.of(CAPABILITY::getDefaultInstance);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
    {
        return cap == CAPABILITY ? instance.cast() : LazyOptional.empty();
    }

    private IShelfPositions getCapability()
    {
        return instance.orElseThrow(BoundTotems.EMPTY_OPTIONAL_EXP);
    }

    @Override
    public INBT serializeNBT()
    {
        return serializeNBT(getCapability());
    }

    public static INBT serializeNBT(IShelfPositions positions)
    {
        return CAPABILITY.getStorage().writeNBT(CAPABILITY, positions, null);
    }

    @Override
    public void deserializeNBT(INBT nbt)
    {
        deserializeNBT(nbt, getCapability());
    }

    public static void deserializeNBT(INBT nbt, IShelfPositions positions)
    {
        CAPABILITY.getStorage().readNBT(CAPABILITY, positions, null, nbt);
    }
}
