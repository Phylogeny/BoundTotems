package com.github.phylogeny.boundtotems.util;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class EntityUtil {
    public static void spawnLightning(BlockState state, ServerLevel world, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt == null)
            return;

        bolt.moveTo(Vec3.upFromBottomCenterOf(pos, 1)
                .subtract(Vec3.atLowerCornerOf(state.getValue(BlockTotemShelf.FACING).getNormal()).scale(0.34375)));
        bolt.setVisualOnly(true);
        world.addFreshEntity(bolt);
    }

    public static double getReach(Player player) {
        return player.getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue() + 1;
    }

    public static BlockHitResult rayTraceBlocks(Player player) {
        Vec3 startPos = player.getEyePosition(1);
        return player.level.clip(new ClipContext(startPos,
                startPos.add(player.getLookAngle().scale(getReach(player))),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    @Nullable
    public static <T extends Entity> T rayTraceEntities(Level world, Player player, Class<? extends T> classEntity) {
        return rayTraceEntities(world, player, classEntity, box -> box);
    }

    @Nullable
    public static <T extends Entity> T rayTraceEntities(Level world, Player player, Class<? extends T> classEntity, UnaryOperator<AABB> boxOperator) {
        double reach = getReach(player);
        Vec3 eyes = player.getEyePosition(1);
        Vec3 look = eyes.add(player.getLookAngle().scale(reach));
        double distShortest = Double.POSITIVE_INFINITY;
        Entity entityHit = null;
        for (Entity entity : world.getEntitiesOfClass(classEntity, player.getBoundingBox().inflate(reach))) {
            Optional<Vec3> hit = boxOperator.apply(entity.getBoundingBox()).clip(eyes, look);
            if (hit.isPresent()) {
                double dist = eyes.distanceToSqr(hit.get());
                if (dist < distShortest) {
                    entityHit = entity;
                    distShortest = dist;
                }
            }
        }
        return (T) entityHit;
    }

    public static void teleportEntity(Entity entity, ResourceKey<Level> dimension, Vec3 pos, float pitch, float yaw) {
        if (!(entity.getCommandSenderWorld() instanceof ServerLevel))
            return;

        ServerLevel worldCurrent = (ServerLevel) entity.getCommandSenderWorld();
        if (dimension != worldCurrent.dimension()) {
            ServerLevel world = worldCurrent.getServer().getLevel(dimension);
            if (world == null) {
                entity.sendMessage(new TranslatableComponent(LangUtil.getKey("chat", "teleport.failed")), Util.NIL_UUID);
                return;
            }
            if (entity instanceof ServerPlayer player)
                player.teleportTo(world, pos.x, pos.y, pos.z, yaw, pitch);
            else {
                entity.changeDimension(world, new ITeleporter() {
                    @Override
                    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                        Entity entityRepos = repositionEntity.apply(false);
                        entityRepos.absMoveTo(pos.x, pos.y, pos.z, yaw, pitch);
                        setPositionAndNullifyMotion(entityRepos, pos);
                        return entityRepos;
                    }
                });
                return;
            }
        }
        entity.setXRot(pitch);
        entity.setYRot(yaw);
        setPositionAndNullifyMotion(entity, pos);
    }

    private static void setPositionAndNullifyMotion(Entity entity, Vec3 pos) {
        entity.teleportTo(pos.x, pos.y, pos.z);
        entity.setDeltaMovement(0, 0, 0);
        entity.fallDistance = 0;
    }
}