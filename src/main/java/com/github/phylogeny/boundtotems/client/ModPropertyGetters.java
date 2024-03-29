package com.github.phylogeny.boundtotems.client;

import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.item.ItemRitualDagger;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModPropertyGetters {
    private static final Map<UUID, CompassData> SHELF_POSITIONS = new ConcurrentHashMap<>();

    public static void addShelfPositions(UUID id, Set<Vec3> positions) {
        if (!SHELF_POSITIONS.containsKey(id))
            SHELF_POSITIONS.put(id, new CompassData());

        SHELF_POSITIONS.get(id).positions = positions;
    }

    private static class CompassData {
        private Set<Vec3> positions;
        private double rotation;
        private double rota;
        private long lastUpdateTick;
    }

    private static class CompassRotationPropertyGetter extends CompassData implements ClampedItemPropertyFunction {
        @Override
        public float unclampedCall(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
            if (entity == null)
                return 0F;

            if (world == null)
                world = (ClientLevel) entity.level;

            return Mth.positiveModulo(getAngleNeedle(stack, world, entity), 1F);
        }

        private float getAngleNeedle(ItemStack stack, Level world, LivingEntity entity) {
            UUID id = NBTUtil.getStackId(stack);
            if (id != null) {
                CompassData compassData = SHELF_POSITIONS.get(id);
                if (compassData != null && !compassData.positions.isEmpty()) {
                    double angleEntity = entity.getYRot();
                    angleEntity = Mth.positiveModulo(angleEntity / 360, 1);
                    double angleSpawn = getAngleToNearestBoundShelf(compassData.positions, entity) / (double) ((float) Math.PI * 2F);
                    return wobble(compassData, world, 0.5 - (angleEntity - 0.25 - angleSpawn));
                }
            }
            return wobble(this, world, Math.random());
        }

        private float wobble(CompassData compassData, Level world, double angle) {
            if (world.getGameTime() != compassData.lastUpdateTick) {
                compassData.lastUpdateTick = world.getGameTime();
                double delta = angle - compassData.rotation;
                delta = Mth.positiveModulo(delta + 0.5, 1) - 0.5;
                compassData.rota += delta * 0.1;
                compassData.rota *= 0.8;
                compassData.rotation = Mth.positiveModulo(compassData.rotation + compassData.rota, 1);
            }
            return (float) compassData.rotation;
        }

        private double getAngleToNearestBoundShelf(Set<Vec3> positions, LivingEntity entity) {
            Vec3 posNearest = null;
            double distance;
            double distanceShortest = Double.POSITIVE_INFINITY;
            for (Vec3 pos : positions) {
                distance = entity.position().distanceToSqr(pos);
                if (distance < distanceShortest) {
                    distanceShortest = distance;
                    posNearest = pos;
                }
            }
            return Math.atan2(posNearest.z() - entity.getZ(), posNearest.x() - entity.getX());
        }
    }

    public static void register() {
        ItemProperties.register(ItemsMod.TOTEM_SHELF_ITEM.get(), new ResourceLocation("type"), (stack, world, entity, seed) ->
                stack.hasTag() && stack.getTag().contains(NBTUtil.GLOWING) ? 1 : 0);

        ItemProperties.register(ItemsMod.RITUAL_DAGGER.get(), new ResourceLocation("state"), (stack, world, entity, seed) ->
                stack.hasTag() && stack.getTag().contains(NBTUtil.GLOWING) ? 3 : ItemRitualDagger.State.get(stack).ordinal());

        ItemProperties.register(ItemsMod.BOUND_COMPASS.get(), new ResourceLocation("angle"), new CompassRotationPropertyGetter());
    }
}