package mrtjp.projectred.core.libmc.recipe.recipes;

import mrtjp.projectred.core.libmc.recipe.item.Input;
import mrtjp.projectred.core.libmc.recipe.item.Output;

import java.io.Serializable;

public class InductiveFurnaceRecipe implements Serializable {
    public Input in;
    public Output out;
    public int burnTime;

    public InductiveFurnaceRecipe(Input in, Output out, int burnTime) {
        this.in = in;
        this.out = out;
        this.burnTime = burnTime;
    }

    public Output createOutput() {
        return out.createOutput();
    }
}