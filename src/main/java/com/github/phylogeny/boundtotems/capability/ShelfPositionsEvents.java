package com.github.phylogeny.boundtotems.capability;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Hashtable;
import java.util.Set;

@Mod.EventBusSubscriber
public class ShelfPositionsEvents {
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity)
            event.addCapability(BoundTotems.getResourceLoc("shelf_dimensions"), new ShelfPositionsProvider());
    }

    @SubscribeEvent
    public static void syncDataForClonedPlayers(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player deadPlayer = event.getOriginal();
            deadPlayer.reviveCaps();
            Hashtable<ResourceLocation, Set<BlockPos>> positions = CapabilityUtil.getShelfPositions(deadPlayer).getPositions();
            CapabilityUtil.getShelfPositions(event.getPlayer()).setPositions(positions);
            deadPlayer.invalidateCaps();
        }
    }
}