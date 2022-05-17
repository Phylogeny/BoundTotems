package com.github.phylogeny.boundtotems.client.jei;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.util.LangUtil;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class InWorldRecipe {
    private final String langKey;
    private Object[] langParameters;
    private List<List<ItemStack>> inputs, outputs;
    private final List<ResourceLocation> slides;
    private ITickTimer timer;

    protected InWorldRecipe(String name, IGuiHelper guiHelper, int slideCount, ItemLike... inputs) {
        this(name, guiHelper, slideCount, itemsToStacks(inputs));
    }

    protected InWorldRecipe(String name, IGuiHelper guiHelper, int slideCount, ItemStack... inputs) {
        this.inputs = new ArrayList<>();
        this.inputs.add(Arrays.asList(inputs));
        langKey = LangUtil.join(InWorldRecipeCategory.LANG_KEY_PREFIX, "recipe", name);
        slides = IntStream.range(0, slideCount).mapToObj(i ->
                BoundTotems.getResourceLoc("textures/jei/" + name + "_" + i + ".png")).collect(Collectors.toList());
        if (slideCount > 1)
            timer = guiHelper.createTickTimer(slideCount * 15, slideCount - 1, false);
    }

    public InWorldRecipe setLangParameters(Object... langParameters) {
        this.langParameters = langParameters;
        return this;
    }

    public InWorldRecipe setOutputs(ItemLike... outputs) {
        return setOutputs(itemsToStacks(outputs));
    }

    public InWorldRecipe setOutputs(ItemStack... outputs) {
        this.outputs = Collections.singletonList(Arrays.asList(outputs));
        return this;
    }

    public InWorldRecipe setAdditionalInputs(ItemLike... inputs) {
        List<List<ItemStack>> inputsAdditional = Arrays.stream(inputs).map(provider -> Collections.singletonList(new ItemStack(provider))).collect(Collectors.toList());
        this.inputs = Stream.of(this.inputs, inputsAdditional).flatMap(Collection::stream).collect(Collectors.toList());
        return this;
    }

    private static ItemStack[] itemsToStacks(ItemLike[] inputs) {
        return Arrays.stream(inputs).map(ItemStack::new).toArray(ItemStack[]::new);
    }

    public void setIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs);
    }

    public ResourceLocation getCurrentSlide() {
        return slides.get(timer == null ? 0 : timer.getValue());
    }

    public List<Component> getTooltip() {
        return Collections.singletonList(new TranslatableComponent(langKey, langParameters));
    }
}