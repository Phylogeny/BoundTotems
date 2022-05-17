package com.github.phylogeny.boundtotems.client.jei;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.util.LangUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fmlclient.gui.GuiUtils;

import java.util.Collections;
import java.util.List;

public class InWorldRecipeCategory implements IRecipeCategory<InWorldRecipe> {
    public static final String NAME = "in_world_crafting";
    public static final String LANG_KEY_PREFIX = LangUtil.getKey(ModIds.JEI_ID, NAME);
    private static final String LANG_KEY = LANG_KEY_PREFIX + ".category";
    public static final ResourceLocation UID = BoundTotems.getResourceLoc(NAME);
    private static final ResourceLocation TEXTURE_GUI = BoundTotems.getResourceLoc("textures/jei/in_world_crafting_gui.png");
    private final IDrawable background, backgroundSlot, arrow;
    private final IGuiHelper guiHelper;

    public InWorldRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        backgroundSlot = guiHelper.createDrawable(new ResourceLocation("minecraft:textures/gui/container/furnace.png"), 111, 30, 26, 26);
        arrow = guiHelper.createDrawable(TEXTURE_GUI, 32, 0, 22, 15);
        background = guiHelper.createBlankDrawable(160, 125);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends InWorldRecipe> getRecipeClass() {
        return InWorldRecipe.class;
    }

    @Override
    public TextComponent getTitle() {
        return new TextComponent(I18n.get(LANG_KEY));
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return guiHelper.createDrawableIngredient(new ItemStack(Blocks.GRASS_BLOCK));
    }

    @Override
    public void setIngredients(InWorldRecipe recipe, IIngredients ingredients) {
        recipe.setIngredients(ingredients);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, InWorldRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
        guiItemStacks.init(0, false, 132, 10);
        guiItemStacks.init(1, true, 117, 75);
        guiItemStacks.set(ingredients);
    }

    @Override
    public void draw(InWorldRecipe recipe, PoseStack poseStack, double mouseX, double mouseY) {
        drawTexture(recipe.getCurrentSlide(), poseStack, 0, 0, 90, 125);
        int offsetX = 100;
        int offsetY = 6;
        backgroundSlot.draw(poseStack, offsetX + 28, offsetY);
        arrow.draw(poseStack, offsetX - 2, offsetY + 5);
        offsetX += 12;
        offsetY += 54;
        Font fr = Minecraft.getInstance().font;
        String tool = I18n.get(LANG_KEY + ".tool");
        fr.draw(poseStack, tool, offsetX + 14 - fr.width(tool) / 2F, offsetY, 0);
        poseStack.pushPose();
        GuiUtils.drawContinuousTexturedBox(poseStack, InWorldRecipeCategory.TEXTURE_GUI, offsetX, offsetY + 10, 0, 0, 28, 28, 32, 16, 2, 0);
        poseStack.popPose();
    }

    private void drawTexture(ResourceLocation texture, PoseStack poseStack, float x, float y, float width, float height) {
        RenderSystem.setShaderTexture(0, texture);
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, x, y + height, 0).uv(0, 1).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).uv(1, 1).endVertex();
        buffer.vertex(matrix, x + width, y, 0).uv(1, 0).endVertex();
        buffer.vertex(matrix, x, y, 0).uv(0, 0).endVertex();
        buffer.end();
        BufferUploader.end(buffer);
    }

    @Override
    public List<Component> getTooltipStrings(InWorldRecipe recipe, double mouseX, double mouseY) {
        return mouseX < 90 ? recipe.getTooltip() : Collections.emptyList();
    }
}