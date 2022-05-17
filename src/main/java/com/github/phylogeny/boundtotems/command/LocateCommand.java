package com.github.phylogeny.boundtotems.command;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelf;
import com.github.phylogeny.boundtotems.util.LangUtil;
import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

public class LocateCommand {
    private static final String NAME = "bound_totem_shelf";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("locate").requires(source -> source.hasPermission(2))
                .then(Commands.literal(NAME).then(Commands.argument("target", EntityArgument.entities())
                        .executes(command -> locateStructure(command, EntityArgument.getEntity(command, "target"))))));
    }

    private static int locateStructure(CommandContext<CommandSourceStack> command, Entity entity) throws CommandSyntaxException {
        BlockPos pos = new BlockPos(command.getSource().getPosition());
        if (!(entity instanceof LivingEntity))
            throwException("args.entity");

        AtomicReference<BlockEntityTotemShelf> nearestShelf = new AtomicReference<>();
        AtomicDouble distanceShortest = new AtomicDouble(Double.POSITIVE_INFINITY);
        BlockEntityTotemShelf.visitTotemShelves((LivingEntity) entity, (world, shelf) -> {
            double distance = shelf.getBlockPos().distSqr(pos);
            if (distance < distanceShortest.get()) {
                distanceShortest.set(distance);
                nearestShelf.set(shelf);
            }
            return new BlockEntityTotemShelf.ShelfVisitationResult(false, true);
        });
        if (nearestShelf.get() == null)
            throwException("result.none", getLocalizedText("none." + (entity == command.getSource().getEntity() ? "self" : "other")));

        BlockPos target = nearestShelf.get().getBlockPos();
        int distance = Mth.floor(getDistance(pos.getX(), pos.getZ(), target.getX(), target.getZ()));
        BlockState state = nearestShelf.get().getBlockState();
        Direction dir = state.getValue(BlockTotemShelf.FACING);
        AABB box = state.getCollisionShape(entity.level, target).bounds();
        double width = dir.getAxis() == Direction.Axis.X ? box.getXsize() : box.getZsize();
        Vec3 exactTarget = new Vec3(target.getX() + 0.5, target.getY() - 1, target.getZ() + 0.5)
                .add(Vec3.atLowerCornerOf(dir.getNormal()).scale(width - 0.5 + entity.getBbWidth() * 0.5));
        Component message = ComponentUtils.wrapInSquareBrackets(new TranslatableComponent("chat.coordinates", target.getX(), exactTarget.y(), target.getZ())).withStyle(text ->
                text.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + exactTarget.x() + " " + exactTarget.y() + " " + exactTarget.z()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("chat.coordinates.tooltip"))));
        command.getSource().sendSuccess(new TranslatableComponent("commands.locate.success", NAME, message, distance), false);
        return distance;
    }

    private static void throwException(String name, Object... args) throws CommandSyntaxException {
        throw new SimpleCommandExceptionType(getLocalizedText(name, args)).create();
    }

    private static TranslatableComponent getLocalizedText(String name, Object... args) {
        return LangUtil.getLocalizedText("command", "shelf.locate." + name, args);
    }

    private static float getDistance(int x1, int z1, int x2, int z2) {
        int dx = x2 - x1;
        int dz = z2 - z1;
        return Mth.sqrt((float) (dx * dx + dz * dz));
    }
}