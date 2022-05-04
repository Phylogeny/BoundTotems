package com.github.phylogeny.boundtotems.command;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelf;
import com.github.phylogeny.boundtotems.util.LangUtil;
import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.concurrent.atomic.AtomicReference;

public class LocateCommand
{
    private static final String NAME = "bound_totem_shelf";

    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(Commands.literal("locate").requires(source -> source.hasPermission(2))
            .then(Commands.literal(NAME).then(Commands.argument("target", EntityArgument.entities())
                        .executes(command -> locateStructure(command, EntityArgument.getEntity(command, "target"))))));
    }

    private static int locateStructure(CommandContext<CommandSource> command, Entity entity) throws CommandSyntaxException
    {
        BlockPos pos = new BlockPos(command.getSource().getPosition());
        if (!(entity instanceof LivingEntity))
            throwException("args.entity");

        AtomicReference<TileEntityTotemShelf> nearestShelf = new AtomicReference<>();
        AtomicDouble distanceShortest = new AtomicDouble(Double.POSITIVE_INFINITY);
        TileEntityTotemShelf.visitTotemShelves((LivingEntity) entity, (world, shelf) ->
        {
            double distance = shelf.getBlockPos().distSqr(pos);
            if (distance < distanceShortest.get())
            {
                distanceShortest.set(distance);
                nearestShelf.set(shelf);
            }
            return new TileEntityTotemShelf.ShelfVisitationResult(false, true);
        });
        if (nearestShelf.get() == null)
            throwException("result.none", getLocalizedText("none." + (entity == command.getSource().getEntity() ? "self" : "other")));

        BlockPos target = nearestShelf.get().getBlockPos();
        int distance = MathHelper.floor(getDistance(pos.getX(), pos.getZ(), target.getX(), target.getZ()));
        BlockState state = nearestShelf.get().getBlockState();
        Direction dir = state.getValue(BlockTotemShelf.FACING);
        AxisAlignedBB box = state.getCollisionShape(entity.level, target).bounds();
        double width = dir.getAxis() == Direction.Axis.X ? box.getXsize() : box.getZsize();
        Vector3d exactTarget = new Vector3d(target.getX() + 0.5, target.getY() - 1, target.getZ() + 0.5)
                .add(Vector3d.atLowerCornerOf(dir.getNormal()).scale(width - 0.5 + entity.getBbWidth() * 0.5));
        ITextComponent message = TextComponentUtils.wrapInSquareBrackets(new TranslationTextComponent("chat.coordinates", target.getX(), exactTarget.y(), target.getZ())).withStyle(text ->
                text.withColor(TextFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + exactTarget.x() + " " + exactTarget.y() + " " + exactTarget.z()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("chat.coordinates.tooltip"))));
        command.getSource().sendSuccess(new TranslationTextComponent("commands.locate.success", NAME, message, distance), false);
        return distance;
    }

    private static void throwException(String name, Object... args) throws CommandSyntaxException
    {
        throw new SimpleCommandExceptionType(getLocalizedText(name, args)).create();
    }

    private static TranslationTextComponent getLocalizedText(String name, Object... args) {
        return LangUtil.getLocalizedText("command", "shelf.locate." + name, args);
    }

    private static float getDistance(int x1, int z1, int x2, int z2)
    {
        int dx = x2 - x1;
        int dz = z2 - z1;
        return MathHelper.sqrt((float)(dx * dx + dz * dz));
    }
}