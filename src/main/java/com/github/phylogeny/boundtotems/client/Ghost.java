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
    private final float velocity, maxLife;
    private Vector3d pos, motion, targetPos;
    private int life, alpha;

    public Ghost(Entity entity, float velocity, int maxLife, @Nullable Vector3d targetPos, @Nullable Entity targetEntity)
    {
        this.entity = entity;
        this.velocity = velocity;
        this.maxLife = life = maxLife;
        if (targetPos != null)
            targetPos = offsetTarget(targetPos);

        this.targetPos = targetPos;
        this.targetEntity = targetEntity;
        pos = entity.getPositionVec();
        updateMotion();
    }

    public boolean update()
    {
        updateMotion();
        pos = pos.add(motion);
        return life-- == 0;
    }

    private void updateMotion()
    {
        if (targetEntity != null)
            targetPos = offsetTarget(targetEntity.getEyePosition(1));

        Vector3d dir = targetPos.subtract(pos);
        motion = dir.normalize().scale(velocity);
        alpha = (int) Math.max(150 - 150 * Math.max(1 - dir.length() + 0.1, 0), 0);
    }

    private Vector3d offsetTarget(Vector3d target)
    {
        return target.subtract(0, entity.getHeight() * (entity instanceof ItemEntity ? 1 : 0.5), 0);
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
        BufferBuilderTransparent.alpha = (int) (alpha * (life / maxLife));
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