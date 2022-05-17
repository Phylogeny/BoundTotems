package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketUpdateBoundCompass;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelf;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemBoundCompass extends CompassItem {
    public ItemBoundCompass(Properties builder) {
        super(builder);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int itemSlot, boolean isSelected) {
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            NBTUtil.setBoundEntity(stack, player);
            NBTUtil.setStackId(stack);
            if (world.getGameTime() % (int) (Config.SERVER.boundCompassSyncInterval.get() * 20) == 0) {
                Set<Vec3> cachedPositions = new HashSet<>();
                entity = NBTUtil.getBoundEntity(stack, player.getLevel());
                if (entity != null) {
                    LivingEntity boundEntity = (LivingEntity) entity;
                    Set<BlockPos> positions = CapabilityUtil.getShelfPositions(boundEntity).getPositions().get(NBTUtil.getDimensionKey(world));
                    if (positions != null && !positions.isEmpty()) {
                        boolean isHolder = boundEntity.getUUID().equals(player.getUUID());
                        positions.forEach(pos -> {
                            BlockEntity te = world.getBlockEntity(pos);
                            if (te instanceof BlockEntityTotemShelf) {
                                BlockEntityTotemShelf shelf = (BlockEntityTotemShelf) te;
                                if (isHolder || player.getUUID().equals(shelf.getOwnerId()))
                                    cachedPositions.add(getCenter(world, pos));
                            }
                        });
                    }
                }
                PacketNetwork.sendTo(new PacketUpdateBoundCompass(cachedPositions, NBTUtil.getStackId(stack)), player);
            }
        }
    }

    private Vec3 getCenter(Level world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).bounds().move(pos).getCenter();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        NBTUtil.addBoundEntityInformation(stack, tooltip);
    }
}