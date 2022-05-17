package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.util.EntityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBoundTotemTeleporting extends ItemBoundTotem {
    public ItemBoundTotemTeleporting(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult result = player.isCrouching() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (!world.isClientSide && result == InteractionResult.SUCCESS) {
            LivingEntity entity = EntityUtil.rayTraceEntities(world, player, LivingEntity.class);
            NBTUtil.setBoundLocation(stack, entity != null ? entity : player);
        }
        return new InteractionResultHolder<>(result, stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, world, entity, itemSlot, isSelected);
        if (!world.isClientSide && !NBTUtil.hasBoundLocation(stack.getTag()) && entity instanceof LivingEntity)
            NBTUtil.setBoundLocation(stack, (LivingEntity) entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        NBTUtil.addBoundLocationInformation(stack, tooltip, flag.isAdvanced());
    }
}