package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketSyncShelfCap;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class ItemBoundCompass extends CompassItem
{
        public ItemBoundCompass(Properties builder)
        {
            super(builder);
            addPropertyOverride(new ResourceLocation("angle"), new IItemPropertyGetter()
            {
                private double rotation;
                private double rota;
                private long lastUpdateTick;

                public float call(ItemStack stack, @Nullable World world, @Nullable LivingEntity entity)
                {
                    if (entity == null)
                        return 0F;

                    if (world == null)
                        world = entity.world;

                    double angleNeedle = getAngleNeedle(stack, world, entity);
                    angleNeedle = wobble(world, angleNeedle);
                    return MathHelper.positiveModulo((float) angleNeedle, 1F);
                }

                private double getAngleNeedle(ItemStack stack, World world, LivingEntity entity)
                {
                    if (entity.getUniqueID().equals(NBTUtil.getBoundEntityId(stack)))
                    {
                        Set<BlockPos> positions = CapabilityUtil.getShelfPositions(entity).getPositions().get(world.getDimension().getType());
                        if (positions != null && !positions.isEmpty())
                        {
                            double angleEntity = entity.rotationYaw;
                            angleEntity = MathHelper.positiveModulo(angleEntity / 360, 1);
                            double angleSpawn = getAngleToNearestBoundShelf(positions, entity) / (double) ((float) Math.PI * 2F);
                            return  0.5 - (angleEntity - 0.25 - angleSpawn);
                        }
                    }
                    return Math.random();
                }

                private double wobble(World world, double angle)
                {
                    if (world.getGameTime() != lastUpdateTick)
                    {
                        lastUpdateTick = world.getGameTime();
                        double delta = angle - rotation;
                        delta = MathHelper.positiveModulo(delta + 0.5, 1) - 0.5;
                        rota += delta * 0.1;
                        rota *= 0.8;
                        rotation = MathHelper.positiveModulo(rotation + rota, 1);
                    }
                    return rotation;
                }

                private double getAngleToNearestBoundShelf(Set<BlockPos> positions, LivingEntity entity)
                {
                    BlockPos posNearest = null;
                    double distance;
                    double distanceShortest = Double.POSITIVE_INFINITY;
                    for (BlockPos pos : positions)
                    {
                        distance = pos.distanceSq(entity.getPosition());
                        if (distance < distanceShortest)
                        {
                            distanceShortest = distance;
                            posNearest = pos;
                        }
                    }
                    return Math.atan2(posNearest.getZ() - entity.getPosZ(), posNearest.getX() - entity.getPosX());
                }
            });
        }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected)
    {
        if (entity instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            NBTUtil.setBoundEntity(stack, player);
            if (world.getGameTime() % (int) (Config.SERVER.boundCompasssSyncInterval.get() * 20) == 0)
                PacketNetwork.sendTo(new PacketSyncShelfCap(player), player);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
        NBTUtil.addBoundEntityInformation(stack, tooltip);
    }
}