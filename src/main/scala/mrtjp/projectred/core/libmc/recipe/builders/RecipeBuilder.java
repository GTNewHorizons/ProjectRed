package mrtjp.projectred.core.libmc.recipe.builders;

import mrtjp.projectred.core.libmc.recipe.item.Input;
import mrtjp.projectred.core.libmc.recipe.item.Output;

import java.util.ArrayList;
import java.util.List;

public abstract class RecipeBuilder {
    protected List<Input> inputs = new ArrayList<>();
    protected List<Output> outputs = new ArrayList<>();

    public RecipeBuilder addInput(Input elem) {
        inputs.add(elem);
        return this;
    }

    public RecipeBuilder addOutput(Output elem) {
        outputs.add(elem);
        return this;
    }

    public List<Input> inResult;
    public List<Output> outResult;

    protected RecipeBuilder compute() {
        inResult = new ArrayList<>(inputs);
        outResult = new ArrayList<>(outputs);
        return this;
    }
}