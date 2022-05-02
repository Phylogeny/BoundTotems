package com.github.phylogeny.boundtotems.client;

import com.github.phylogeny.boundtotems.util.ReflectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Ghost
{
    private static final Field LAYER_RENDERERS = ObfuscationReflectionHelper.findField(LivingRenderer.class, "field_177097_h");
    public static final Field GAME_TYPE = ObfuscationReflectionHelper.findField(NetworkPlayerInfo.class, "field_178866_b");
    private final Entity entity, targetEntity;
    private final float velocity, halfEntityWidth;
    private Vector3d pos, motion, targetPos;
    private int alpha;

    public Ghost(Entity entity, float velocity, @Nullable Vector3d targetPos, @Nullable Entity targetEntity)
    {
        this.entity = entity;
        this.velocity = velocity * 2;
        halfEntityWidth = entity.getWidth() / 2F;
        if (targetPos != null)
            targetPos = offsetTarget(targetPos);

        this.targetPos = targetPos;
        this.targetEntity = targetEntity;
        pos = entity.getPositionVec();
        updateMotion();
    }

    public boolean update()
    {
        boolean dead = updateMotion();
        pos = pos.add(motion);
        return dead;
    }

    private boolean updateMotion()
    {
        if (targetEntity != null)
            targetPos = offsetTarget(targetEntity.getEyePosition(1));

        Vector3d dir = targetPos.subtract(pos);
        motion = dir.normalize().scale(velocity);
        if (targetEntity != null && dir.length() > 5)
            motion = motion.add(dir.normalize().scale(dir.length() - 5));
        alpha = (int) (150 * MathHelper.clamp(dir.length() / 2D - halfEntityWidth, 0, 1));
        return dir.length() <= velocity;
    }

    private Vector3d offsetTarget(Vector3d target)
    {
        return target.subtract(0, entity.getHeight() * (entity instanceof ItemEntity ? 1.3 : 0.5), 0);
    }

    public void render(RenderWorldLastEvent event)
    {
        IRenderTypeBuffer.Impl typeBuffer = BufferBuilderTransparent.getRenderTypeBuffer();
        float partialTicks = event.getPartialTicks();
        Vector3d pos = this.pos.add(motion.scale(partialTicks));
        Vector3d posCamera = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
        double dx = pos.x - posCamera.x;
        double dy = pos.y - posCamera.y;
        double dz = pos.z - posCamera.z;
        float f = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        BufferBuilderTransparent.alpha = alpha;
        EntityRendererManager rendererManager = Minecraft.getInstance().getRenderManager();
        rendererManager.setRenderShadow(false);
        int packedLight = 15728880;
        if (entity instanceof ItemEntity)
            rendererManager.renderEntityStatic(entity, dx, dy, dz, f, partialTicks, event.getMatrixStack(), typeBuffer, packedLight);
        else
        {
            EntityRenderer<? super Entity> renderer = rendererManager.getRenderer(entity);
            if (renderer instanceof LivingRenderer)
            {
                LivingRenderer rendererLiving = (LivingRenderer) renderer;
                List<LayerRenderer> layers = (List<LayerRenderer>) ReflectionUtil.getValue(LAYER_RENDERERS, rendererLiving);
                ReflectionUtil.setValue(LAYER_RENDERERS, rendererLiving, new ArrayList<>());
                RenderType renderType = rendererLiving.getEntityModel().getRenderType(rendererLiving.getEntityTexture(entity));
                if (entity instanceof PlayerEntity && !renderType.toString().contains("cutout"))
                    rendererManager.renderEntityStatic(entity, dx, dy, dz, f, partialTicks, event.getMatrixStack(), typeBuffer, packedLight);
                else
                {
                    ClientPlayNetHandler connection = Minecraft.getInstance().getConnection();
                    if (connection != null)
                    {
                        NetworkPlayerInfo playerInfo = connection.getPlayerInfo(ClientEvents.getPlayer().getGameProfile().getId());
                        if (playerInfo != null)
                        {
                            GameType gameType = playerInfo.getGameType();
                            ReflectionUtil.setValue(GAME_TYPE, playerInfo, GameType.SPECTATOR);
                            boolean isInvisible = entity.isInvisible();
                            entity.setInvisible(true);
                            rendererManager.renderEntityStatic(entity, dx, dy, dz, f, partialTicks, event.getMatrixStack(), typeBuffer, packedLight);
                            ReflectionUtil.setValue(GAME_TYPE, playerInfo, gameType);
                            entity.setInvisible(isInvisible);
                        }
                    }
                }
                ReflectionUtil.setValue(LAYER_RENDERERS, rendererLiving, layers);
            }
        }
        typeBuffer.finish();
        rendererManager.setRenderShadow(true);
    }
}