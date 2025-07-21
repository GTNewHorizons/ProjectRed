package mrtjp.projectred.core.libmc.recipe.builders;

import cpw.mods.fml.common.registry.GameRegistry;
import mrtjp.projectred.core.libmc.recipe.recipes.ShapelessBuilderRecipe;

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