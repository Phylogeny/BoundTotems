package com.github.phylogeny.boundtotems.capability;

import com.github.phylogeny.boundtotems.BoundTotems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShelfPositionsProvider implements ICapabilitySerializable<CompoundTag> {
    public static Capability<IShelfPositions> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    private final LazyOptional<IShelfPositions> instance = LazyOptional.of(ShelfPositions::new);

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(ShelfPositionsProvider.class);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CAPABILITY ? instance.cast() : LazyOptional.empty();
    }

    private IShelfPositions getCapability() {
        return instance.orElseThrow(BoundTotems.EMPTY_OPTIONAL_EXP);
    }

    @Override
    public CompoundTag serializeNBT() {
        ListTag dimensions = new ListTag();
        ListTag positions = new ListTag();
        getCapability().getPositions().forEach((dimension, posSet) -> {
            dimensions.add(StringTag.valueOf(dimension.toString()));
            ListTag posSetList = new ListTag();
            posSet.forEach(pos -> posSetList.add(LongTag.valueOf(pos.asLong())));
            positions.add(posSetList);
        });
        CompoundTag nbt = new CompoundTag();
        nbt.put("shelf_dimensions", dimensions);
        nbt.put("shelf_positions", positions);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ListTag dimensions = nbt.getList("shelf_dimensions", Tag.TAG_STRING);
        ListTag positions = nbt.getList("shelf_positions", Tag.TAG_LIST);
        Hashtable<ResourceLocation, Set<BlockPos>> positionsTable = new Hashtable<>();
        for (int i = 0; i < dimensions.size(); i++) {
            Set<BlockPos> posSet = new HashSet<>();
            positions.getList(i).forEach(element -> posSet.add(BlockPos.of(((LongTag) element).getAsLong())));
            positionsTable.put(new ResourceLocation(dimensions.get(i).getAsString()), posSet);
        }
        getCapability().setPositions(positionsTable);
    }
}