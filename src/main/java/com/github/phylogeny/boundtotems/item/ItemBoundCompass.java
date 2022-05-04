package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketUpdateBoundCompass;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelf;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemBoundCompass extends CompassItem
{
    public ItemBoundCompass(Properties builder)
    {
        super(builder);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected)
    {
        if (entity instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            NBTUtil.setBoundEntity(stack, player);
            NBTUtil.setStackId(stack);
            if (world.getGameTime() % (int) (Config.SERVER.boundCompassSyncInterval.get() * 20) == 0) {
                Set<Vector3d> cachedPositions = new HashSet<>();
                entity = NBTUtil.getBoundEntity(stack, player.getLevel());
                if (entity != null) {
                    LivingEntity boundEntity = (LivingEntity) entity;
                    Set<BlockPos> positions = CapabilityUtil.getShelfPositions(boundEntity).getPositions().get(NBTUtil.getDimensionKey(world));
                    if (positions != null && !positions.isEmpty()) {
                        boolean isHolder = boundEntity.getUUID().equals(player.getUUID());
                        positions.forEach(pos -> {
                            TileEntity te = world.getBlockEntity(pos);
                            if (te instanceof TileEntityTotemShelf)
                            {
                                TileEntityTotemShelf shelf = (TileEntityTotemShelf) te;
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

    private Vector3d getCenter(World world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).bounds().move(pos).getCenter();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
        NBTUtil.addBoundEntityInformation(stack, tooltip);
    }
}