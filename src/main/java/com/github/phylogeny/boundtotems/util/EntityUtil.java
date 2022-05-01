package com.github.phylogeny.boundtotems.util;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class EntityUtil
{
    public static void spawnLightning(BlockState state, ServerWorld world, BlockPos pos)
    {
        LightningBoltEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt == null)
            return;

        bolt.moveForced(Vector3d.copyCenteredWithVerticalOffset(pos, 1)
                .subtract(Vector3d.copy(state.get(BlockTotemShelf.FACING).getDirectionVec()).scale(0.34375)));
        bolt.setEffectOnly(true);
        world.addEntity(bolt);
    }

    public static double getReach(PlayerEntity player)
    {
        return player.getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue() + 1;
    }

    public static BlockRayTraceResult rayTraceBlocks(PlayerEntity player)
    {
        Vector3d startPos = player.getEyePosition(1);
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
        Vector3d eyes = player.getEyePosition(1);
        Vector3d look = eyes.add(player.getLookVec().scale(reach));
        double distShortest = Double.POSITIVE_INFINITY;
        Entity entityHit = null;
        for (Entity entity : world.getEntitiesWithinAABB(classEntity, player.getBoundingBox().grow(reach)))
        {
            Optional<Vector3d> hit = boxOperator.apply(entity.getBoundingBox()).rayTrace(eyes, look);
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

    public static void teleportEntity(Entity entity, RegistryKey<World> dimension, Vector3d pos, float pitch, float yaw)
    {
        if (!(entity.getEntityWorld() instanceof ServerWorld))
            return;

        ServerWorld worldCurrent = (ServerWorld) entity.getEntityWorld();
        if (dimension != worldCurrent.getDimensionKey())
        {
            ServerWorld world = worldCurrent.getServer().getWorld(dimension);
            if (world == null)
            {
                entity.sendMessage(new TranslationTextComponent(LangUtil.getKey("chat", "teleport.failed")), Util.DUMMY_UUID);
                return;
            }
            if (entity instanceof ServerPlayerEntity)
                ((ServerPlayerEntity) entity).teleport(world, pos.x, pos.y, pos.z, yaw, pitch);
            else
            {
                entity.changeDimension(world, new ITeleporter()
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

    private static void setPositionAndNullifyMotion(Entity entity, Vector3d pos)
    {
        entity.setPositionAndUpdate(pos.x, pos.y, pos.z);
        entity.setMotion(0, 0, 0);
        entity.fallDistance = 0;
    }
}