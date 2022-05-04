package com.github.phylogeny.boundtotems.client;

import com.github.phylogeny.boundtotems.util.ReflectionUtil;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class BufferBuilderTransparent extends BufferBuilder
{
    private static final Constructor<IRenderTypeBuffer.Impl> RENDER_TYPE_BUFFER = ObfuscationReflectionHelper.findConstructor(IRenderTypeBuffer.Impl.class, BufferBuilder.class, Map.class);
    private static final Supplier<IRenderTypeBuffer.Impl> TYPE_BUFFER = Suppliers.memoize(BufferBuilderTransparent::createRenderTypeBuffer);
    public static final BufferBuilderTransparent INSTANCE = new BufferBuilderTransparent(2097152);

    private static IRenderTypeBuffer.Impl createRenderTypeBuffer()
    {
        return ReflectionUtil.getNewInstance(RENDER_TYPE_BUFFER, BufferBuilderTransparent.INSTANCE, new HashMap<>());
    }

    public static IRenderTypeBuffer.Impl getRenderTypeBuffer()
    {
        return TYPE_BUFFER.get();
    }

    /**
     *
     */
    public static int alpha;

    public BufferBuilderTransparent(int bufferSize)
    {
        super(bufferSize);
    }

    @Override
    public IVertexBuilder color(int red, int green, int blue, int alpha)
    {
        return super.color(red, green, blue, BufferBuilderTransparent.alpha);
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ)
    {
        super.vertex(x, y, z, red, green, blue, BufferBuilderTransparent.alpha / 255F, texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ);
    }
}