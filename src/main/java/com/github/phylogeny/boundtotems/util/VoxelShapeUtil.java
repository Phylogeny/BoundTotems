package com.github.phylogeny.boundtotems.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.DirectionProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VoxelShapeUtil
{
    public static Collection<VoxelShape> makeCuboidShape(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return Collections.singletonList(Block.makeCuboidShape(x1, y1, z1, x2, y2, z2));
    }

    public static ImmutableMap<BlockState, VoxelShape> getTransformedShapes(ImmutableMap<BlockState, VoxelShape> shapes, java.util.function.Function<VoxelShape, VoxelShape> transform)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = new ImmutableMap.Builder<>();
        shapes.forEach(((state, shape) -> builder.put(state, transform.apply(shape))));
        return builder.build();
    }

    public static ImmutableMap<BlockState, VoxelShape> generateShapes(ImmutableList<BlockState> states, DirectionProperty facing, Function<BlockState, Collection<VoxelShape>> factory)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = new ImmutableMap.Builder<>();
        states.forEach(state -> builder.put(state, combineAll(getRotatedVoxelShapes(factory.apply(state))
                .stream().map(shapes -> shapes[state.get(facing).getHorizontalIndex()]).collect(Collectors.toList()))));
        return builder.build();
    }

    public static VoxelShape combineAll(Collection<VoxelShape> shapes)
    {
        return VoxelShapes.or(VoxelShapes.empty(), shapes.toArray(new VoxelShape[0]));
    }

    public static Collection<VoxelShape[]> getRotatedVoxelShapes(Collection<VoxelShape> shapes)
    {
        return shapes.stream().map(shape -> IntStream.range(0, 4)
                .mapToObj(index -> rotateShape(shape, Direction.byHorizontalIndex((index + 3) % 4))).toArray(VoxelShape[]::new)).collect(Collectors.toList());
    }

    public static VoxelShape rotateShape(VoxelShape shape, Direction facing)
    {
        double startX = shape.getStart(Axis.X);
        double startZ = shape.getStart(Axis.Z);
        double endX = shape.getEnd(Axis.X);
        double endZ = shape.getEnd(Axis.Z);
        switch(facing)
        {
            case WEST:
                double tempStartX = startX;
                double tempStartZ = startZ;
                startX = 1 - endX;
                startZ = 1 - endZ;
                endX = 1 - tempStartX;
                endZ = 1 - tempStartZ;
                break;
            case NORTH:
                tempStartX = startX;
                startX = startZ;
                startZ = 1 - endX;
                endX = endZ;
                endZ = 1 - tempStartX;
                break;
            case SOUTH:
                tempStartX = startX;
                tempStartZ = startZ;
                double tempEndX = endX;
                startX = 1 - endZ;
                startZ = tempStartX;
                endX = 1 - tempStartZ;
                endZ = tempEndX;
                break;
            default:
        }
        return VoxelShapes.create(startX, shape.getStart(Axis.Y), startZ, endX, shape.getEnd(Axis.Y), endZ);
    }
}