package com.github.phylogeny.boundtotems.client.renderer;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.client.BufferBuilderTransparent;
import com.github.phylogeny.boundtotems.init.BlocksMod;
import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.init.BlockEntitiesMod;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelf;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelfBinding;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.items.IItemHandler;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RendererBlockEntityTotemShelf implements BlockEntityRenderer<BlockEntityTotemShelf> {
    private final Supplier<ItemStack> daggerStack = Suppliers.memoize(() -> new ItemStack(ItemsMod.RITUAL_DAGGER.get()));
    private final Supplier<ItemStack> totemShelfStack = Suppliers.memoize(() -> new ItemStack(BlocksMod.TOTEM_SHELF.get()));

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        register(event, BlockEntitiesMod.TOTEM_SHELF);
        register(event, BlockEntitiesMod.TOTEM_SHELF_BINDING);
    }

    private static <T extends BlockEntityTotemShelf> void register(EntityRenderersEvent.RegisterRenderers event, RegistryObject<BlockEntityType<T>> type) {
        event.registerBlockEntityRenderer(type.get(), RendererBlockEntityTotemShelf::new);
    }

    public RendererBlockEntityTotemShelf(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BlockEntityTotemShelf totemShelf, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        poseStack.pushPose(); {
            BlockPos pos = totemShelf.getBlockPos();
            Level world = totemShelf.getLevel();
            Integer alpha = null;
            MultiBufferSource.BufferSource typeBuffer = BufferBuilderTransparent.getRenderTypeBuffer();
            poseStack.pushPose(); {
                poseStack.translate(0.50002, 0.003125 - 0.5, 0.50002);
                Direction facing = totemShelf.getBlockState().getValue(BlockTotemShelf.FACING);
                if (facing.getAxis() == Axis.X)
                    facing = facing.getOpposite();

                poseStack.mulPose(Vector3f.YP.rotationDegrees(facing.toYRot()));
                if (totemShelf instanceof BlockEntityTotemShelfBinding bindingShelf) {
                    alpha = (int) (255 * bindingShelf.getBindingPercentage());
                    renderItemGlowing(totemShelfStack.get(), world, TransformType.NONE, poseStack, typeBuffer, combinedOverlay, alpha);
                }
                IItemHandler inventory = CapabilityUtil.getInventory(totemShelf);
                int size = inventory.getSlots();
                poseStack.translate(-0.18, 0.96875, -0.3);
                float scaleTotems = 0.7F;
                poseStack.scale(scaleTotems, scaleTotems, scaleTotems);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        poseStack.pushPose(); {
                            poseStack.translate((i % 2) * 0.5, -(i / 2) * (0.625 / scaleTotems), 0);
                            renderItem(stack, world, poseStack, buffer, combinedLight, combinedOverlay);
                        }
                        poseStack.popPose();
                    }
                }
            }
            poseStack.popPose();
            Vec3 knifePos = totemShelf.getKnifePos();
            if (knifePos != null) {
                poseStack.translate(knifePos.x - pos.getX(), knifePos.y - pos.getY(), knifePos.z - pos.getZ());
                poseStack.mulPose(Vector3f.YP.rotationDegrees(-90));
                Vec3 dir = totemShelf.getKnifeDirection();
                assert dir != null;
                poseStack.mulPose(Vector3f.YP.rotation((float) Math.atan2(dir.x, dir.z)));
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(-45));
                poseStack.mulPose(Vector3f.ZP.rotation((float) Math.asin(dir.y)));
                poseStack.translate(0, -0.15, 0);
                BufferBuilderTransparent.alpha = 255;
                renderItem(totemShelf.getKnife(), world, poseStack, alpha != null ? typeBuffer : buffer, combinedLight, combinedOverlay);
                if (alpha != null)
                    renderItemGlowing(totemShelf.getKnife().copy(), world, TransformType.GROUND, poseStack, typeBuffer, combinedOverlay, alpha);
            }
        }
        poseStack.popPose();
    }

    public void renderItem(ItemStack stack, Level world, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, world, null, 0);
        Minecraft.getInstance().getItemRenderer().render(stack, TransformType.GROUND, false, poseStack, buffer, combinedLight, combinedOverlay, model);
    }

    public void renderItemGlowing(ItemStack stack, Level world, TransformType transformType, PoseStack poseStack, MultiBufferSource.BufferSource typeBuffer, int combinedOverlay, Integer alpha) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        stack.getOrCreateTag().putByte(NBTUtil.GLOWING, (byte) 0);
        BakedModel model = itemRenderer.getModel(stack, world, null, 0);
        BufferBuilderTransparent.alpha = alpha;
        itemRenderer.render(daggerStack.get(), transformType, false, poseStack, typeBuffer, 15728880, combinedOverlay, model);
        typeBuffer.endBatch();
    }
}