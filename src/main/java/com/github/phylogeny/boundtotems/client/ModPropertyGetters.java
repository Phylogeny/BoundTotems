package com.github.phylogeny.boundtotems.client;

import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.item.ItemRitualDagger;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModPropertyGetters {
    private static final Map<UUID, CompassData> SHELF_POSITIONS = new ConcurrentHashMap<>();

    public static void addShelfPositions(UUID id, Set<Vector3d> positions) {
        if (!SHELF_POSITIONS.containsKey(id))
            SHELF_POSITIONS.put(id, new CompassData());

        SHELF_POSITIONS.get(id).positions = positions;
    }

    private static class CompassData {
        private Set<Vector3d> positions;
        private double rotation;
        private double rota;
        private long lastUpdateTick;
    }

    private static class CompassRotationPropertyGetter extends CompassData implements IItemPropertyGetter {
        @Override
        public float call(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity) {
            if (entity == null)
                return 0F;

            if (world == null)
                world = (ClientWorld) entity.level;

            return MathHelper.positiveModulo(getAngleNeedle(stack, world, entity), 1F);
        }

        private float getAngleNeedle(ItemStack stack, World world, LivingEntity entity) {
            UUID id = NBTUtil.getStackId(stack);
            if (id != null) {
                CompassData compassData = SHELF_POSITIONS.get(id);
                if (compassData != null && !compassData.positions.isEmpty()) {
                    double angleEntity = entity.yRot;
                    angleEntity = MathHelper.positiveModulo(angleEntity / 360, 1);
                    double angleSpawn = getAngleToNearestBoundShelf(compassData.positions, entity) / (double) ((float) Math.PI * 2F);
                    return wobble(compassData, world, 0.5 - (angleEntity - 0.25 - angleSpawn));
                }
            }
            return wobble(this, world, Math.random());
        }

        private float wobble(CompassData compassData, World world, double angle) {
            if (world.getGameTime() != compassData.lastUpdateTick) {
                compassData.lastUpdateTick = world.getGameTime();
                double delta = angle - compassData.rotation;
                delta = MathHelper.positiveModulo(delta + 0.5, 1) - 0.5;
                compassData.rota += delta * 0.1;
                compassData.rota *= 0.8;
                compassData.rotation = MathHelper.positiveModulo(compassData.rotation + compassData.rota, 1);
            }
            return (float) compassData.rotation;
        }

        private double getAngleToNearestBoundShelf(Set<Vector3d> positions, LivingEntity entity) {
            Vector3d posNearest = null;
            double distance;
            double distanceShortest = Double.POSITIVE_INFINITY;
            for (Vector3d pos : positions) {
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
        ItemModelsProperties.register(ItemsMod.TOTEM_SHELF_ITEM.get(), new ResourceLocation("type"), (stack, world, entity) ->
                stack.hasTag() && stack.getTag().contains(NBTUtil.GLOWING) ? 1 : 0);

        ItemModelsProperties.register(ItemsMod.RITUAL_DAGGER.get(), new ResourceLocation("state"), (stack, world, entity) ->
                stack.hasTag() && stack.getTag().contains(NBTUtil.GLOWING) ? 3 : ItemRitualDagger.State.get(stack).ordinal());

        ItemModelsProperties.register(ItemsMod.BOUND_COMPASS.get(), new ResourceLocation("angle"), new CompassRotationPropertyGetter());
    }
}