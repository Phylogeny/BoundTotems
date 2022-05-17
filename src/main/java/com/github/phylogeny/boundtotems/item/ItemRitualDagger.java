package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketAddGhost;
import com.github.phylogeny.boundtotems.util.EntityUtil;
import com.github.phylogeny.boundtotems.util.LangUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class ItemRitualDagger extends Item {
    public static final String NAME = "ritual_dagger";

    public ItemRitualDagger(Properties properties) {
        super(properties.stacksTo(1));
    }

    public enum State {
        BASE, BLOODY, BOUND;

        private final String langKey;

        State() {
            langKey = LangUtil.getKey("item", NAME, name().toLowerCase());
        }

        public String getLangKey() {
            return langKey;
        }

        public static State get(ItemStack stack) {
            if (!NBTUtil.hasBoundEntity(stack.getTag()))
                return BASE;

            return NBTUtil.isKnifeBound(stack.getTag()) ? BOUND : BLOODY;
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot != EquipmentSlot.MAINHAND || State.get(stack) != State.BOUND ? super.getAttributeModifiers(slot, stack)
                : ImmutableMultimap.of(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 5, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return State.get(stack).getLangKey();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        NBTUtil.addBoundEntityInformation(stack, tooltip);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        InteractionResult result = InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            setBoundEntity(stack, player, player, true);
            result = InteractionResult.SUCCESS;
        } else if (NBTUtil.hasBoundEntity(stack)) {
            ItemEntity entityTotem = EntityUtil.rayTraceEntities(world, player, ItemEntity.class, box -> box.expandTowards(0, 0.25, 0));
            ItemStack stackBound = ItemsMod.getBoundItem(entityTotem);
            if (!stackBound.isEmpty()) {
                if (!world.isClientSide) {
                    entityTotem.setItem(NBTUtil.copyBoundEntity(stack, stackBound));
                    sendGhostPacket(player, entityTotem);
                }
                result = InteractionResult.SUCCESS;
                player.swing(hand);
            }
        }
        return new InteractionResultHolder<>(result, stack);
    }

    private boolean setBoundEntity(ItemStack stack, Player player, Entity entity, boolean attackSelf) {
        if (!(player instanceof ServerPlayer) || !(entity instanceof LivingEntity))
            return false;

        if (attackSelf) {
            player.attack(player);
            player.resetAttackStrengthTicker();
            return true;
        }
        return NBTUtil.setBoundEntity(stack, (LivingEntity) entity);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (setBoundEntity(stack, player, entity, false))
            sendGhostPacket(player, entity);

        return false;
    }

    private void sendGhostPacket(Player player, Entity entity) {
        PacketNetwork.sendToAllTrackingAndSelf(new PacketAddGhost(entity, 0.2F, null, player), entity);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (State.get(context.getItemInHand()) == State.BLOODY && NBTUtil.hasBoundEntity(context.getItemInHand())) {
            BlockState state = context.getLevel().getBlockState(context.getClickedPos());
            if (state.getBlock() instanceof LayeredCauldronBlock && state.getValue(LayeredCauldronBlock.LEVEL) > 0) {
                LayeredCauldronBlock.lowerFillLevel(state, context.getLevel(), context.getClickedPos());
                context.getItemInHand().getOrCreateTag().remove(NBTUtil.BOUND_ENTITY);
                context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}