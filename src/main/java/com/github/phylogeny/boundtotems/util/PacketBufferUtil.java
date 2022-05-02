package com.github.phylogeny.boundtotems.util;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

public class PacketBufferUtil
{
    public static <T> void writeNullableObject(PacketBuffer buf, @Nullable T object, Consumer<T> bufWriter)
    {
        boolean nonNull = object != null;
        buf.writeBoolean(nonNull);
        if (nonNull)
            bufWriter.accept(object);
    }

    @Nullable
    public static <T> T readNullableObject(PacketBuffer buf, Function<PacketBuffer, T> bufReader)
    {
        return buf.readBoolean() ? bufReader.apply(buf) : null;
    }

    public static void writeVec(PacketBuffer buf, Vector3d vec)
    {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    public static Vector3d readVec(PacketBuffer buf)
    {
        return new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void writeAABB(PacketBuffer buf, AxisAlignedBB box)
    {
        buf.writeDouble(box.minX);
        buf.writeDouble(box.minY);
        buf.writeDouble(box.minZ);
        buf.writeDouble(box.maxX);
        buf.writeDouble(box.maxY);
        buf.writeDouble(box.maxZ);
    }

    public static AxisAlignedBB readAABB(PacketBuffer buf)
    {
        return new AxisAlignedBB(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}