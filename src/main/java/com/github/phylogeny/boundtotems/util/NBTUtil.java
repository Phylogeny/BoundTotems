package com.github.phylogeny.boundtotems.util;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NBTUtil
{
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
    public static final String UUID = "uuid";

    public static boolean hasBoundEntity(ItemStack stack)
    {
        return hasBoundEntity(stack.getTag());
    }

    public static boolean hasBoundEntity(CompoundNBT nbt)
    {
        return nbt != null && nbt.contains(BOUND_ENTITY);
    }

    public static UUID readUniqueId(CompoundNBT nbt)
    {
        return nbt.hasUniqueId(UUID) ? nbt.getUniqueId(UUID) : Util.DUMMY_UUID;
    }

    public static CompoundNBT writeUniqueId(UUID id)
    {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putUniqueId(UUID, id);
        return nbt;
    }

    @Nullable
    public static UUID getBoundEntityId(ItemStack stack)
    {
        CompoundNBT nbt = stack.getChildTag(BOUND_ENTITY);
        return nbt == null ? null : readUniqueId(nbt);
    }

    @Nullable
    public static LivingEntity getBoundEntity(ItemStack stack, ServerWorld world)
    {
        UUID id = getBoundEntityId(stack);
        if (id != null)
        {
            Entity entity = world.getEntityByUuid(id);
            if (entity != null)
                return (LivingEntity) entity;
        }
        return null;
    }

    @Nullable
    public static String getBoundEntityName(ItemStack stack)
    {
        CompoundNBT nbt = stack.getChildTag(BOUND_ENTITY);
        return nbt == null ? null : nbt.getString(ENTITY_NAME);
    }

    public static ItemStack copyBoundEntity(ItemStack source, ItemStack target)
    {
        CompoundNBT nbt = source.getChildTag(BOUND_ENTITY);
        if (nbt != null)
            setBoundEntity(target, readUniqueId(nbt), nbt.getString(ENTITY_NAME));

        return target;
    }

    public static boolean setBoundEntity(ItemStack stack, LivingEntity entity)
    {
        if (hasBoundEntity(stack))
            return false;

        setBoundEntity(stack, entity.getUniqueID(), entity.getDisplayName().getString());
        return true;
    }

    public static void setBoundEntity(ItemStack stack, UUID entityId, String entityDisplayName)
    {
        CompoundNBT nbt = writeUniqueId(entityId);
        nbt.putString(ENTITY_NAME, entityDisplayName);
        stack.setTagInfo(BOUND_ENTITY, nbt);
    }

    public static boolean matchesBoundEntity(ItemStack stack, LivingEntity entity)
    {
        UUID entityId = getBoundEntityId(stack);
        return entityId != null && entity.getUniqueID().equals(entityId);
    }

    public static void addBoundEntityInformation(ItemStack stack, List<ITextComponent> tooltip)
    {
        String name = getBoundEntityName(stack);
        if (name != null)
        {
            LangUtil.addTooltip(tooltip, "entity.name", name);
            if (Screen.hasShiftDown())
                LangUtil.addTooltip(tooltip, "entity.uuid", getBoundEntityId(stack));
        }
    }

    public static boolean hasBoundLocation(CompoundNBT nbt)
    {
        return nbt != null && nbt.contains(BOUND_LOCATION);
    }

    public static void teleportEntity(ItemStack stack, LivingEntity entity)
    {
        CompoundNBT nbt = stack.getChildTag(BOUND_LOCATION);
        if (nbt != null)
            EntityUtil.teleportEntity(entity, getDimension(new ResourceLocation(nbt.getString(DIMENSION))), readVec(nbt), nbt.getFloat(PITCH), nbt.getFloat(YAW));
    }

    public static void copyBoundLocation(ItemStack source, ItemStack target)
    {
        CompoundNBT nbt = source.getChildTag(BOUND_LOCATION);
        if (nbt != null)
            setBoundLocation(target, readVec(nbt), nbt.getString(DIMENSION), nbt.getFloat(PITCH), nbt.getFloat(YAW));
    }

    public static void setBoundLocation(ItemStack stack, LivingEntity entity)
    {
        setBoundLocation(stack, entity.getPositionVec(),
                getDimensionKey(entity.getEntityWorld()).toString(), entity.rotationPitch, entity.rotationYaw);
    }

    public static void setBoundLocation(ItemStack stack, Vector3d pos, String dimension, float pitch, float yaw)
    {
        CompoundNBT nbt = writeVec(pos);
        nbt.putString(DIMENSION, dimension);
        nbt.putFloat(PITCH, pitch);
        nbt.putFloat(YAW, yaw);
        stack.setTagInfo(BOUND_LOCATION, nbt);
    }

    public static RegistryKey<World> getDimension(ResourceLocation key)
    {
        return RegistryKey.getOrCreateKey(Registry.WORLD_KEY, key);
    }

    public static ResourceLocation getDimensionKey(World world)
    {
        return world.getDimensionKey().getLocation();
    }

    private static Vector3d readVec(CompoundNBT nbt)
    {
        return new Vector3d(nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
    }

    private static CompoundNBT writeVec(Vector3d vec)
    {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putDouble("X", vec.x);
        nbt.putDouble("Y", vec.y);
        nbt.putDouble("Z", vec.z);
        return nbt;
    }

    public static void addBoundLocationInformation(ItemStack stack, List<ITextComponent> tooltip, boolean fullDimensionName)
    {
        CompoundNBT nbt = stack.getChildTag(BOUND_LOCATION);
        if (nbt == null)
            return;

        Vector3d pos = readVec(nbt);
        String dimension = nbt.getString(DIMENSION);
        if (!fullDimensionName)
            dimension = dimension.substring(dimension.indexOf(":") + 1);

        LangUtil.addTooltip(tooltip, "location", dimension,
                Integer.toString((int) pos.x), Integer.toString((int) pos.y), Integer.toString((int) pos.z));
    }

    public static boolean isKnifeBound(CompoundNBT nbt)
    {
        return nbt != null && nbt.getBoolean(BOUND_KNIFE);
    }

    public static void bindKnife(CompoundNBT nbt)
    {
        nbt.putBoolean(BOUND_KNIFE, true);
    }

    public static void writeObjectToSubTag(CompoundNBT nbt, String key, Consumer<CompoundNBT> nbtWriter)
    {
        CompoundNBT nbtSub = new CompoundNBT();
        nbtWriter.accept(nbtSub);
        nbt.put(key, nbtSub);
    }

    public static <T> T readObjectFromSubTag(CompoundNBT nbt, String key, Function<CompoundNBT, T> nbtReader)
    {
        return nbtReader.apply(nbt.getCompound(key));
    }

    public static void writeNullableObject(Object object, Runnable nbtWriter)
    {
        if (object != null)
            nbtWriter.run();
    }

    @Nullable
    public static <T> T readNullableObject(CompoundNBT nbt, String key, Supplier<T> nbtReader)
    {
        return nbt.contains(key) ? nbtReader.get() : null;
    }
}