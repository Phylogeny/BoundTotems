package com.github.phylogeny.boundtotems.util;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class NBTUtil {
    public static final String BOUND_KNIFE = "bound_knife";
    public static final String BOUND_ENTITY = "bound_entity";
    public static final String BOUND_LOCATION = "bound_location";
    public static final String ENTITY_NAME = "name";
    public static final String DIMENSION = "dim";
    public static final String PITCH = "pitch";
    public static final String YAW = "yaw";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";
    public static final String POSITION = "pos";
    public static final String DIRECTION = "dir";
    public static final String KNIFE = "knife";
    public static final String GLOWING = "glowing";
    public static final String ID = "uuid";
    public static final String STACK_ID = "stack_id";

    public static boolean hasBoundEntity(ItemStack stack) {
        return hasBoundEntity(stack.getTag());
    }

    public static boolean hasBoundEntity(CompoundTag nbt) {
        return nbt != null && nbt.contains(BOUND_ENTITY);
    }

    public static UUID readUniqueId(CompoundTag nbt) {
        return nbt.hasUUID(ID) ? nbt.getUUID(ID) : Util.NIL_UUID;
    }

    public static CompoundTag writeUniqueId(UUID id) {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(ID, id);
        return nbt;
    }

    public static void setStackId(ItemStack stack) {
        if (!stack.getOrCreateTag().contains(STACK_ID))
            stack.addTagElement(STACK_ID, writeUniqueId(UUID.randomUUID()));
    }

    @Nullable
    public static UUID getStackId(ItemStack stack) {
        return getId(stack, STACK_ID);
    }

    @Nullable
    public static UUID getBoundEntityId(ItemStack stack) {
        return getId(stack, BOUND_ENTITY);
    }

    @Nullable
    private static UUID getId(ItemStack stack, String key) {
        CompoundTag nbt = stack.getTagElement(key);
        return nbt == null ? null : readUniqueId(nbt);
    }

    @Nullable
    public static LivingEntity getBoundEntity(ItemStack stack, ServerLevel world) {
        UUID id = getBoundEntityId(stack);
        if (id != null) {
            Entity entity = world.getEntity(id);
            if (entity != null)
                return (LivingEntity) entity;
        }
        return null;
    }

    @Nullable
    public static String getBoundEntityName(ItemStack stack) {
        CompoundTag nbt = stack.getTagElement(BOUND_ENTITY);
        return nbt == null ? null : nbt.getString(ENTITY_NAME);
    }

    public static ItemStack copyBoundEntity(ItemStack source, ItemStack target) {
        CompoundTag nbt = source.getTagElement(BOUND_ENTITY);
        if (nbt != null)
            setBoundEntity(target, readUniqueId(nbt), nbt.getString(ENTITY_NAME));

        return target;
    }

    public static boolean setBoundEntity(ItemStack stack, LivingEntity entity) {
        if (hasBoundEntity(stack))
            return false;

        setBoundEntity(stack, entity.getUUID(), entity.getDisplayName().getString());
        return true;
    }

    public static void setBoundEntity(ItemStack stack, UUID entityId, String entityDisplayName) {
        CompoundTag nbt = writeUniqueId(entityId);
        nbt.putString(ENTITY_NAME, entityDisplayName);
        stack.addTagElement(BOUND_ENTITY, nbt);
    }

    public static boolean matchesBoundEntity(ItemStack stack, LivingEntity entity) {
        UUID entityId = getBoundEntityId(stack);
        return entityId != null && entity.getUUID().equals(entityId);
    }

    public static void addBoundEntityInformation(ItemStack stack, List<Component> tooltip) {
        String name = getBoundEntityName(stack);
        if (name != null) {
            LangUtil.addTooltipWithFormattedSuffix(tooltip, "entity.name", name, ChatFormatting.GRAY);
            if (Screen.hasShiftDown())
                LangUtil.addTooltipWithFormattedSuffix(tooltip, "entity.uuid", Objects.requireNonNull(getBoundEntityId(stack)).toString(), ChatFormatting.GRAY);
            else
                LangUtil.addTooltip(tooltip, "entity.more_info");
        }
    }

    public static boolean hasBoundLocation(CompoundTag nbt) {
        return nbt != null && nbt.contains(BOUND_LOCATION);
    }

    public static void teleportEntity(ItemStack stack, LivingEntity entity) {
        CompoundTag nbt = stack.getTagElement(BOUND_LOCATION);
        if (nbt != null)
            EntityUtil.teleportEntity(entity, getDimension(new ResourceLocation(nbt.getString(DIMENSION))), readVec(nbt), nbt.getFloat(PITCH), nbt.getFloat(YAW));
    }

    public static void copyBoundLocation(ItemStack source, ItemStack target) {
        CompoundTag nbt = source.getTagElement(BOUND_LOCATION);
        if (nbt != null)
            setBoundLocation(target, readVec(nbt), nbt.getString(DIMENSION), nbt.getFloat(PITCH), nbt.getFloat(YAW));
    }

    public static void setBoundLocation(ItemStack stack, LivingEntity entity) {
        setBoundLocation(stack, entity.position(),
                getDimensionKey(entity.getCommandSenderWorld()).toString(), entity.getXRot(), entity.getYRot());
    }

    public static void setBoundLocation(ItemStack stack, Vec3 pos, String dimension, float pitch, float yaw) {
        CompoundTag nbt = writeVec(pos);
        nbt.putString(DIMENSION, dimension);
        nbt.putFloat(PITCH, pitch);
        nbt.putFloat(YAW, yaw);
        stack.addTagElement(BOUND_LOCATION, nbt);
    }

    public static ResourceKey<Level> getDimension(ResourceLocation key) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, key);
    }

    public static ResourceLocation getDimensionKey(Level world) {
        return world.dimension().location();
    }

    private static Vec3 readVec(CompoundTag nbt) {
        return new Vec3(nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
    }

    private static CompoundTag writeVec(Vec3 vec) {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("X", vec.x);
        nbt.putDouble("Y", vec.y);
        nbt.putDouble("Z", vec.z);
        return nbt;
    }

    public static void addBoundLocationInformation(ItemStack stack, List<Component> tooltip, boolean fullDimensionName) {
        CompoundTag nbt = stack.getTagElement(BOUND_LOCATION);
        if (nbt == null)
            return;

        Vec3 pos = readVec(nbt);
        String dimension = nbt.getString(DIMENSION);
        if (!fullDimensionName)
            dimension = dimension.substring(dimension.indexOf(":") + 1);

        LangUtil.addTooltip(tooltip, "location", dimension,
                Integer.toString((int) pos.x), Integer.toString((int) pos.y), Integer.toString((int) pos.z));
    }

    public static boolean isKnifeBound(CompoundTag nbt) {
        return nbt != null && nbt.getBoolean(BOUND_KNIFE);
    }

    public static void bindKnife(CompoundTag nbt) {
        nbt.putBoolean(BOUND_KNIFE, true);
    }

    public static void writeObjectToSubTag(CompoundTag nbt, String key, Consumer<CompoundTag> nbtWriter) {
        CompoundTag nbtSub = new CompoundTag();
        nbtWriter.accept(nbtSub);
        nbt.put(key, nbtSub);
    }

    public static <T> T readObjectFromSubTag(CompoundTag nbt, String key, Function<CompoundTag, T> nbtReader) {
        return nbtReader.apply(nbt.getCompound(key));
    }

    public static <T> void writeNullableObject(@Nullable T object, Consumer<T> nbtWriter) {
        Optional.ofNullable(object).ifPresent(nbtWriter);
    }

    @Nullable
    public static <T> T readNullableObject(CompoundTag nbt, String key, Function<String, T> nbtReader) {
        return nbt.contains(key) ? nbtReader.apply(key) : null;
    }
}