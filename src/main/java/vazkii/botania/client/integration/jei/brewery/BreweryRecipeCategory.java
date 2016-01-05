package vazkii.botania.client.integration.jei.brewery;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class BreweryRecipeCategory implements IRecipeCategory {

    private final IDrawableStatic background;
    private final String localizedName;

    private static final int slotContainerInput = 0;
    private static final int slotIngredient1 = 1;
    private static final int slotIngredient2 = 2;
    private static final int slotIngredient3 = 3;
    private static final int slotIngredient4 = 4;
    private static final int slotIngredient5 = 5;
    private static final int slotIngredient6 = 6;
    private static final int slotOutput = 7;

    public BreweryRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = new ResourceLocation("botania", "textures/gui/neiBrewery.png");
        background = guiHelper.createDrawable(location, 0, 0, 166, 65, 0, 0, 0, 0);
        localizedName = StatCollector.translateToLocal("botania.nei.brewery");
    }

    @Nonnull
    @Override
    public String getUid() {
        return "botania.brewery";
    }

    @Nonnull
    @Override
    public String getTitle() {
        return localizedName;
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {}

    @Override
    public void drawAnimations(Minecraft minecraft) {}

    @Override
    public void setRecipe(@Nonnull IRecipeLayout recipeLayout, @Nonnull IRecipeWrapper recipeWrapper) {
        if (!(recipeWrapper instanceof BreweryRecipeWrapper))
            return;
        BreweryRecipeWrapper wrapper = ((BreweryRecipeWrapper) recipeWrapper);

        List inputs = wrapper.getInputs();

        recipeLayout.getItemStacks().init(0, true, 39, 41);
        if (inputs.get(0) instanceof ItemStack) {
            recipeLayout.getItemStacks().set(0, ((ItemStack) inputs.get(0)));
        } else {
            recipeLayout.getItemStacks().set(0, ((Collection<ItemStack>) inputs.get(0)));
        }

        int index = 1, posX = 60;
        for (Object o : wrapper.getInputs()) {
            recipeLayout.getItemStacks().init(index, true, posX, 6);
            if (o instanceof ItemStack) {
                recipeLayout.getItemStacks().set(index, ((ItemStack) o));
            } else if (o instanceof Collection) {
                recipeLayout.getItemStacks().set(index, ((Collection<ItemStack>) o));
            }
            index++;
            posX += 18;
        }

        recipeLayout.getItemStacks().init(7, false, 87, 41);
        recipeLayout.getItemStacks().set(7, wrapper.getOutputs().get(0));
    }
}