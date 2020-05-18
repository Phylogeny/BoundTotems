package com.github.phylogeny.boundtotems.capability;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Hashtable;
import java.util.Set;

@Mod.EventBusSubscriber
public class ShelfPositionsEvents
{
    public static void register()
    {
        CapabilityManager.INSTANCE.register(IShelfPositions.class, new ShelfPositionsStorage(), ShelfPositions::new);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof LivingEntity)
            event.addCapability(new ResourceLocation(BoundTotems.MOD_ID, "shelf_dimensions"), new ShelfPositionsProvider());
    }

    @SubscribeEvent
    public static void syncDataForClonedPlayers(PlayerEvent.Clone event)
    {
        if (event.isWasDeath())
        {
            Hashtable<DimensionType, Set<BlockPos>> positions = CapabilityUtil.getShelfPositions(event.getOriginal()).getPositions();
            CapabilityUtil.getShelfPositions(event.getPlayer()).setPositions(positions);
        }
    }
}