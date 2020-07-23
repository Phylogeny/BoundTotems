package com.github.phylogeny.boundtotems.util;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class EntityUtil
{
    public static void spawnLightning(BlockState state, ServerWorld world, BlockPos pos)
    {
        Vec3i offset = state.get(BlockTotemShelf.FACING).getDirectionVec();
        LightningBoltEntity bolt = new LightningBoltEntity(world, pos.getX() + 0.5 - offset.getX() * 0.34375, pos.up().getY(), pos.getZ() + 0.5 - offset.getZ() * 0.34375, true);
        world.addLightningBolt(bolt);
    }

    public static double getReach(PlayerEntity player)
    {
        return player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue() + 1;
    }

    public static BlockRayTraceResult rayTraceBlocks(PlayerEntity player)
    {
        Vec3d startPos = player.getEyePosition(1);
        return player.world.rayTraceBlocks(new RayTraceContext(startPos,
                startPos.add(player.getLookVec().scale(getReach(player))),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
    }

    @Nullable
    public static <T extends Entity> T rayTraceEntities(World world, PlayerEntity player, Class<? extends T> classEntity)
    {
        return rayTraceEntities(world, player, classEntity, box -> box);
    }

    @Nullable
    public static <T extends Entity> T rayTraceEntities(World world, PlayerEntity player, Class<? extends T> classEntity, UnaryOperator<AxisAlignedBB> boxOperator)
    {
        double reach = getReach(player);
        Vec3d eyes = player.getEyePosition(1);
        Vec3d look = eyes.add(player.getLookVec().scale(reach));
        double distShortest = Double.POSITIVE_INFINITY;
        Entity entityHit = null;
        for (Entity entity : world.getEntitiesWithinAABB(classEntity, player.getBoundingBox().grow(reach)))
        {
            Optional<Vec3d> hit = boxOperator.apply(entity.getBoundingBox()).rayTrace(eyes, look);
            if (hit.isPresent())
            {
                double dist = eyes.squareDistanceTo(hit.get());
                if (dist < distShortest)
                {
                    entityHit = entity;
                    distShortest = dist;
                }
            }
        }
        return (T) entityHit;
    }

    public static void teleportEntity(Entity entity, DimensionType dimension, Vec3d pos, float pitch, float yaw)
    {
        if (!(entity.getEntityWorld() instanceof ServerWorld))
            return;

        ServerWorld worldCurrent = (ServerWorld) entity.getEntityWorld();
        if (dimension != worldCurrent.dimension.getDimension().getType())
        {
            if (entity instanceof ServerPlayerEntity)
                ((ServerPlayerEntity) entity).teleport(worldCurrent.getServer().getWorld(dimension), pos.x, pos.y, pos.z, yaw, pitch);
            else
            {
                entity.changeDimension(dimension, new ITeleporter()
                {
                    @Override
                    public Entity placeEntity(Entity entity, ServerWorld currentWorld, ServerWorld destWorld, float yaw, Function<Boolean, Entity> repositionEntity)
                    {
                        Entity entityRepos = repositionEntity.apply(false);
                        entityRepos.setPositionAndRotation(pos.x, pos.y, pos.z, yaw, pitch);
                        setPositionAndNullifyMotion(entityRepos, pos);
                        return entityRepos;
                    }
                });
                return;
            }
        }
        entity.rotationPitch = pitch;
        entity.rotationYaw = yaw;
        setPositionAndNullifyMotion(entity, pos);
    }

    private static void setPositionAndNullifyMotion(Entity entity, Vec3d pos)
    {
        entity.setPositionAndUpdate(pos.x, pos.y, pos.z);
        entity.setMotion(0, 0, 0);
        entity.fallDistance = 0;
    }
}