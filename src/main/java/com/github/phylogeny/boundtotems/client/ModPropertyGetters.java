package com.github.phylogeny.boundtotems.client;

import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.item.ItemRitualDagger;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Set;

public class ModPropertyGetters {

    public static void register() {
        ItemModelsProperties.registerProperty(ItemsMod.TOTEM_SHELF_ITEM.get(), new ResourceLocation("type"), (stack, world, entity) ->
                stack.hasTag() && stack.getTag().contains(NBTUtil.GLOWING) ? 1 : 0);

        ItemModelsProperties.registerProperty(ItemsMod.RITUAL_DAGGER.get(), new ResourceLocation("state"), (stack, world, entity) ->
                stack.hasTag() && stack.getTag().contains(NBTUtil.GLOWING) ? 3 : ItemRitualDagger.State.get(stack).ordinal());

        ItemModelsProperties.registerProperty(ItemsMod.BOUND_COMPASS.get(), new ResourceLocation("angle"), new IItemPropertyGetter()
        {
            private double rotation;
            private double rota;
            private long lastUpdateTick;

            @Override
            public float call(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity)
            {
                if (entity == null)
                    return 0F;

                if (world == null)
                    world = (ClientWorld) entity.world;

                double angleNeedle = getAngleNeedle(stack, world, entity);
                angleNeedle = wobble(world, angleNeedle);
                return MathHelper.positiveModulo((float) angleNeedle, 1F);
            }

            private double getAngleNeedle(ItemStack stack, World world, LivingEntity entity)
            {
                if (entity.getUniqueID().equals(NBTUtil.getBoundEntityId(stack)))
                {
                    Set<BlockPos> positions = CapabilityUtil.getShelfPositions(entity).getPositions().get(NBTUtil.getDimensionKey(world));
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
}