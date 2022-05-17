package com.github.phylogeny.boundtotems.client;

import com.github.phylogeny.boundtotems.util.ReflectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Ghost {
    private static final Field LAYERS = ObfuscationReflectionHelper.findField(LivingEntityRenderer.class, "f_115291_");
    public static final Field GAME_MODE = ObfuscationReflectionHelper.findField(PlayerInfo.class, "f_105300_");
    private final Entity entity, targetEntity;
    private final float velocity, halfEntityWidth;
    private Vec3 pos, motion, targetPos;
    private int alpha;

    public Ghost(Entity entity, float velocity, @Nullable Vec3 targetPos, @Nullable Entity targetEntity) {
        this.entity = entity;
        this.velocity = velocity;
        halfEntityWidth = entity.getBbWidth() / 2F;
        if (targetPos != null)
            targetPos = offsetTarget(targetPos);

        this.targetPos = targetPos;
        this.targetEntity = targetEntity;
        pos = entity.position();
        updateMotion();
    }

    public boolean update() {
        boolean dead = updateMotion();
        pos = pos.add(motion);
        return dead;
    }

    private boolean updateMotion() {
        if (targetEntity != null)
            targetPos = offsetTarget(targetEntity.getEyePosition(1));

        Vec3 dir = targetPos.subtract(pos);
        motion = dir.normalize().scale(velocity);
        if (targetEntity != null && dir.length() > 5)
            motion = motion.add(dir.normalize().scale(dir.length() - 5));
        alpha = (int) (150 * Mth.clamp(dir.length() / 2D - halfEntityWidth, 0, 1));
        return dir.length() <= velocity;
    }

    private Vec3 offsetTarget(Vec3 target) {
        return target.subtract(0, entity.getBbHeight() * (entity instanceof ItemEntity ? 1.3 : 0.5), 0);
    }

    public void render(RenderWorldLastEvent event) {
        MultiBufferSource.BufferSource typeBuffer = BufferBuilderTransparent.getRenderTypeBuffer();
        float partialTicks = event.getPartialTicks();
        Vec3 pos = this.pos.add(motion.scale(partialTicks));
        Vec3 posCamera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = pos.x - posCamera.x;
        double dy = pos.y - posCamera.y;
        double dz = pos.z - posCamera.z;
        float f = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
        BufferBuilderTransparent.alpha = alpha;
        EntityRenderDispatcher rendererDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        rendererDispatcher.setRenderShadow(false);
        int packedLight = 15728880;
        if (entity instanceof ItemEntity)
            rendererDispatcher.render(entity, dx, dy, dz, f, partialTicks, event.getMatrixStack(), typeBuffer, packedLight);
        else {
            EntityRenderer<? super Entity> renderer = rendererDispatcher.getRenderer(entity);
            if (renderer instanceof LivingEntityRenderer) {
                LivingEntityRenderer rendererLiving = (LivingEntityRenderer) renderer;
                List<RenderLayer> layers = (List<RenderLayer>) ReflectionUtil.getValue(Ghost.LAYERS, rendererLiving);
                ReflectionUtil.setValue(Ghost.LAYERS, rendererLiving, new ArrayList<>());
                RenderType renderType = rendererLiving.getModel().renderType(rendererLiving.getTextureLocation(entity));
                if (entity instanceof Player && !renderType.toString().contains("cutout"))
                    rendererDispatcher.render(entity, dx, dy, dz, f, partialTicks, event.getMatrixStack(), typeBuffer, packedLight);
                else {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        PlayerInfo playerInfo = connection.getPlayerInfo(ClientEvents.getPlayer().getGameProfile().getId());
                        if (playerInfo != null) {
                            GameType gameType = playerInfo.getGameMode();
                            ReflectionUtil.setValue(GAME_MODE, playerInfo, GameType.SPECTATOR);
                            boolean isInvisible = entity.isInvisible();
                            entity.setInvisible(true);
                            Entity leashHolder = null;
                            if (entity instanceof Mob mob && mob.isLeashed()) {
                                leashHolder = mob.getLeashHolder();
                                mob.setDelayedLeashHolderId(0);
                            }
                            rendererDispatcher.render(entity, dx, dy, dz, f, partialTicks, event.getMatrixStack(), typeBuffer, packedLight);
                            if (leashHolder != null)
                                ((Mob) entity).setLeashedTo(leashHolder, false);

                            ReflectionUtil.setValue(GAME_MODE, playerInfo, gameType);
                            entity.setInvisible(isInvisible);
                        }
                    }
                }
                ReflectionUtil.setValue(Ghost.LAYERS, rendererLiving, layers);
            }
        }
        typeBuffer.endBatch();
        rendererDispatcher.setRenderShadow(true);
    }
}