package mrtjp.projectred.core.libmc.recipe.builders;


import mrtjp.projectred.core.libmc.recipe.recipes.InductiveFurnaceRecipe;
import mrtjp.projectred.expansion.InductiveFurnaceRecipeLib;

public class InductiveFurnaceRecipeBuilder extends RecipeBuilder {
    private int ticks = 0;

    public InductiveFurnaceRecipeBuilder setBurnTime(int t) {
        this.ticks = t;
        return this;
    }

    public InductiveFurnaceRecipe result() {
        compute();
        return new InductiveFurnaceRecipe(inResult.get(0), outResult.get(0), ticks);
    }

    public void registerResult() {
        InductiveFurnaceRecipeLib.addRecipe(result());
    }
}