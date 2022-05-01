package com.github.phylogeny.boundtotems.client.jei;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.util.LangUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

public class InWorldRecipeCategory implements IRecipeCategory<InWorldRecipe>
{
    public static final String NAME = "in_world_crafting";
    public static final String LANG_KEY_PREFIX = LangUtil.getKey(ModIds.JEI_ID, NAME);
    private static final String LANG_KEY = LANG_KEY_PREFIX + ".category";
    public static final ResourceLocation UID = BoundTotems.getResourceLoc(NAME);
    private static final ResourceLocation TEXTURE_GUI = BoundTotems.getResourceLoc("textures/jei/in_world_crafting_gui.png");
    private final IDrawable background, backgroundSlot, arrow;
    private final IGuiHelper guiHelper;

    public InWorldRecipeCategory(IGuiHelper guiHelper)
    {
        this.guiHelper = guiHelper;
        backgroundSlot = guiHelper.createDrawable(new ResourceLocation("minecraft:textures/gui/container/furnace.png"), 111, 30, 26, 26);
        arrow = guiHelper.createDrawable(TEXTURE_GUI, 32, 0, 22, 15);
        background = guiHelper.createBlankDrawable(160, 125);
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    @Override
    public Class<? extends InWorldRecipe> getRecipeClass()
    {
        return InWorldRecipe.class;
    }

    @Override
    public String getTitle()
    {
        return I18n.format(LANG_KEY);
    }

    @Override
    public IDrawable getBackground()
    {
        return background;
    }

    @Override
    public IDrawable getIcon()
    {
        return guiHelper.createDrawableIngredient(new ItemStack(Blocks.GRASS_BLOCK));
    }

    @Override
    public void setIngredients(InWorldRecipe recipe, IIngredients ingredients)
    {
        recipe.setIngredients(ingredients);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, InWorldRecipe recipe, IIngredients ingredients)
    {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
        guiItemStacks.init(0, false, 132, 10);
        guiItemStacks.init(1, true, 117, 75);
        guiItemStacks.set(ingredients);
    }

    @Override
    public void draw(InWorldRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY)
    {
        drawTexture(recipe.getCurrentSlide(), matrixStack, 0, 0, 90, 125);
        int offsetX = 100;
        int offsetY = 6;
        backgroundSlot.draw(matrixStack, offsetX + 28, offsetY);
        arrow.draw(matrixStack, offsetX - 2, offsetY + 5);
        offsetX += 12;
        offsetY += 54;
        FontRenderer fr = Minecraft.getInstance().fontRenderer;
        String tool = I18n.format(LANG_KEY + ".tool");
        fr.drawString(matrixStack, tool, offsetX + 14 - fr.getStringWidth(tool) / 2F, offsetY, 0);
        matrixStack.push();
        GuiUtils.drawContinuousTexturedBox(matrixStack, InWorldRecipeCategory.TEXTURE_GUI, offsetX, offsetY + 10, 0, 0, 28, 28, 32, 16, 2, 0);
        matrixStack.pop();
    }

    private void drawTexture(ResourceLocation texture, MatrixStack matrixStack, float x, float y, float width, float height)
    {
        Minecraft.getInstance().getTextureManager().bindTexture(texture);
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(matrix, x, y + height, 0).tex(0, 1).endVertex();
        buffer.pos(matrix, x + width, y + height, 0).tex(1, 1).endVertex();
        buffer.pos(matrix, x + width, y, 0).tex(1, 0).endVertex();
        buffer.pos(matrix, x, y, 0).tex(0, 0).endVertex();
        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);
    }

    @Override
    public List<ITextComponent> getTooltipStrings(InWorldRecipe recipe, double mouseX, double mouseY)
    {
        return mouseX < 90 ? recipe.getTooltip() : Collections.emptyList();
    }
}