package mrtjp.projectred.core.libmc.recipe.builders;

import mrtjp.projectred.core.libmc.recipe.recipes.ShapelessBuilderRecipe;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ShapelessRecipeBuilder extends RecipeBuilder {
    public ShapelessBuilderRecipe result() {
        compute();
        return new ShapelessBuilderRecipe(this);
    }

    public ShapelessRecipeBuilder registerResult() {
        GameRegistry.addRecipe(result());
        return this;
    }
}