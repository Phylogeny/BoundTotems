package com.github.phylogeny.boundtotems.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class PacketBufferUtil {
    public static <T> void writeNullableObject(FriendlyByteBuf buf, @Nullable T object, Consumer<T> bufWriter) {
        Optional<T> nullable = Optional.ofNullable(object);
        buf.writeBoolean(nullable.isPresent());
        nullable.ifPresent(bufWriter);
    }

    @Nullable
    public static <T> T readNullableObject(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> bufReader) {
        return buf.readBoolean() ? bufReader.apply(buf) : null;
    }

    public static void writeVec(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    public static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void writeAABB(FriendlyByteBuf buf, AABB box) {
        buf.writeDouble(box.minX);
        buf.writeDouble(box.minY);
        buf.writeDouble(box.minZ);
        buf.writeDouble(box.maxX);
        buf.writeDouble(box.maxY);
        buf.writeDouble(box.maxZ);
    }

    public static AABB readAABB(FriendlyByteBuf buf) {
        return new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}