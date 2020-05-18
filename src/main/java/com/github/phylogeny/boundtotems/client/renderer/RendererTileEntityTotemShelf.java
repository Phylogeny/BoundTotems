package com.github.phylogeny.boundtotems.client.renderer;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.client.BufferBuilderTransparent;
import com.github.phylogeny.boundtotems.init.BlocksMod;
import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelf;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelfBinding;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.IItemHandler;

import java.util.function.Supplier;

public class RendererTileEntityTotemShelf extends TileEntityRenderer<TileEntityTotemShelf>
{
    private final Supplier<ItemStack> daggerStack = Suppliers.memoize(() -> new ItemStack(ItemsMod.RITUAL_DAGGER.get()));
    private final Supplier<ItemStack> totemShelfStack = Suppliers.memoize(() -> new ItemStack(BlocksMod.TOTEM_SHELF.get()));

    public RendererTileEntityTotemShelf(TileEntityRendererDispatcher rendererDispatcher)
    {
        super(rendererDispatcher);
    }

    @Override
    public void render(TileEntityTotemShelf totemShelf, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay)
    {
        matrixStack.push();
        {
            BlockPos pos = totemShelf.getPos();
            Integer alpha = null;
            IRenderTypeBuffer.Impl typeBuffer = BufferBuilderTransparent.getRenderTypeBuffer();
            matrixStack.push();
            {
                matrixStack.translate(0.50002,  0.003125 - 0.5, 0.50002);
                Direction facing = totemShelf.getBlockState().get(BlockTotemShelf.FACING);
                if (facing.getAxis() == Axis.X)
                    facing = facing.getOpposite();

                matrixStack.rotate(Vector3f.YP.rotation((float) Math.toRadians(facing.getHorizontalAngle())));
                if (totemShelf instanceof TileEntityTotemShelfBinding)
                {
                    alpha = (int) (255 * ((TileEntityTotemShelfBinding) totemShelf).getBindingPercentage());
                    renderItemGlowing(totemShelfStack.get(), TransformType.NONE, matrixStack, typeBuffer, combinedOverlay, alpha);
                }
                IItemHandler inventory = CapabilityUtil.getInventory(totemShelf);
                int size = inventory.getSlots();
                matrixStack.translate(-0.18, 0.96875, -0.3);
                float scaleTotems = 0.7F;
                matrixStack.scale(scaleTotems, scaleTotems, scaleTotems);
                for (int i = 0; i < size; i++)
                {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty())
                    {
                        matrixStack.push();
                        {
                            matrixStack.translate((i % 2) * 0.5, -(i / 2) * (0.625 / scaleTotems), 0);
                            renderItem(stack, matrixStack, buffer, combinedLight, combinedOverlay);
                        }
                        matrixStack.pop();
                    }
                }
            }
            matrixStack.pop();
            Vec3d knifePos = totemShelf.getKnifePos();
            if (knifePos != null)
            {
                matrixStack.translate(knifePos.x - pos.getX(), knifePos.y - pos.getY(), knifePos.z - pos.getZ());
                matrixStack.rotate(Vector3f.YP.rotation((float) Math.toRadians(-90)));
                Vec3d dir = totemShelf.getKnifeDirection();
                assert dir != null;
                matrixStack.rotate(Vector3f.YP.rotation((float) Math.atan2(dir.x, dir.z)));
                matrixStack.rotate(Vector3f.ZP.rotation((float) Math.toRadians(-45)));
                matrixStack.rotate(Vector3f.ZP.rotation((float) Math.asin(dir.y)));
                matrixStack.translate(0, -0.15, 0);
                BufferBuilderTransparent.alpha = 255;
                renderItem(totemShelf.getKnife(), matrixStack, alpha != null ? typeBuffer : buffer, combinedLight, combinedOverlay);
                if (alpha != null)
                    renderItemGlowing(totemShelf.getKnife().copy(), TransformType.GROUND, matrixStack, typeBuffer, combinedOverlay, alpha);
            }
        }
        matrixStack.pop();
    }

    public void renderItem(ItemStack stack, MatrixStack matrixStack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay)
    {
        IBakedModel model = Minecraft.getInstance().getItemRenderer().getItemModelWithOverrides(stack, renderDispatcher.world, null);
        Minecraft.getInstance().getItemRenderer().renderItem(stack, TransformType.GROUND, false, matrixStack, buffer, combinedLight, combinedOverlay, model);
    }

    public void renderItemGlowing(ItemStack stack, TransformType transformType, MatrixStack matrixStack, IRenderTypeBuffer.Impl typeBuffer, int combinedOverlay, Integer alpha)
    {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        stack.getOrCreateTag().putByte(NBTUtil.GLOWING, (byte) 0);
        IBakedModel model = itemRenderer.getItemModelWithOverrides(stack, renderDispatcher.world, null);
        BufferBuilderTransparent.alpha = alpha;
        itemRenderer.renderItem(daggerStack.get(), transformType, false, matrixStack, typeBuffer, 15728880, combinedOverlay, model);
        typeBuffer.finish();
    }
}