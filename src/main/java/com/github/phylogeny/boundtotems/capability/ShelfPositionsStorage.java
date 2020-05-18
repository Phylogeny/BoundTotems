package com.github.phylogeny.boundtotems.capability;

import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.nbt.*;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class ShelfPositionsStorage implements Capability.IStorage<IShelfPositions>
{
    @Nullable
    @Override
    public INBT writeNBT(Capability<IShelfPositions> capability, IShelfPositions instance, Direction side)
    {
        ListNBT dimensions = new ListNBT();
        ListNBT positions = new ListNBT();
        instance.getPositions().forEach((dimension, posSet) ->
        {
            dimensions.add(StringNBT.valueOf(NBTUtil.getDimensionName(dimension)));
            ListNBT posSetList = new ListNBT();
            posSet.forEach(pos -> posSetList.add(LongNBT.valueOf(pos.toLong())));
            positions.add(posSetList);
        });
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("shelf_dimensions", dimensions);
        nbt.put("shelf_positions", positions);
        return nbt;
    }

    @Override
    public void readNBT(Capability<IShelfPositions> capability, IShelfPositions instance, Direction side, INBT nbt)
    {
        if (!(nbt instanceof CompoundNBT))
            return;

        CompoundNBT nbtCompound = (CompoundNBT) nbt;
        ListNBT dimensions = nbtCompound.getList("shelf_dimensions", Constants.NBT.TAG_STRING);
        ListNBT positions = nbtCompound.getList("shelf_positions", Constants.NBT.TAG_LIST);
        Hashtable<DimensionType, Set<BlockPos>> positionsTable = new Hashtable<>();
        for (int i = 0; i < dimensions.size(); i++)
        {
            Set<BlockPos> posSet = new HashSet<>();
            positions.getList(i).forEach(element -> posSet.add(BlockPos.fromLong(((LongNBT) element).getLong())));
            positionsTable.put(NBTUtil.getDimension(dimensions.get(i).getString()), posSet);
        }
        instance.setPositions(positionsTable);
    }
}
