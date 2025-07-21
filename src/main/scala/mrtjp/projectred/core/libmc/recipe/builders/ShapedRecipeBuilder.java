package mrtjp.projectred.core.libmc.recipe.builders;

import cpw.mods.fml.common.registry.GameRegistry;
import mrtjp.projectred.core.libmc.recipe.recipes.ShapedBuilderRecipe;

public class ShapedRecipeBuilder extends TMappedRecipeBuilder {
    public int size = 3;

    public ShapedRecipeBuilder warp(int s) {
        size = s;
        return this;
    }

    public ShapedBuilderRecipe result() {
        compute();
        return new ShapedBuilderRecipe(this);
    }

    public ShapedRecipeBuilder registerResult() {
        GameRegistry.addRecipe(result());
        return this;
    }
}
