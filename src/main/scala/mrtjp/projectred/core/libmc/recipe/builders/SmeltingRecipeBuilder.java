package mrtjp.projectred.core.libmc.recipe.builders;

import cpw.mods.fml.common.registry.GameRegistry;
import mrtjp.projectred.core.libmc.recipe.item.Input;

public class SmeltingRecipeBuilder extends RecipeBuilder {
    private float xp = 0.0f;

    public SmeltingRecipeBuilder setXP(float i) {
        this.xp = i;
        return this;
    }

    public void registerResults() {
        compute();

        for (Input input : inResult) {
            GameRegistry.addSmelting(input.matchingInputs().get(0), outResult.get(0).createOutput(), xp);
        }
    }
}