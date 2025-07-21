package mrtjp.projectred.core.libmc.recipe.builders;

import mrtjp.projectred.core.libmc.recipe.item.Input;
import mrtjp.projectred.core.libmc.recipe.item.Output;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

abstract class TMappedRecipeBuilder extends RecipeBuilder {
    public String map = "";

    public TMappedRecipeBuilder map(String m) {
        this.map = m;
        return this;
    }

    public Map<Integer, Input> inputMap;
    public Map<Integer, Output> outputMap;

    @Override
    protected RecipeBuilder compute() {
        super.compute();

        Map<Integer, Input> inMB = new HashMap<>();
        Map<Integer, Output> outMB = new HashMap<>();
        String[] sSeq = map.split("");

        for (int i = 0; i < sSeq.length; i++) {
            String id = sSeq[i];
            if (!id.isEmpty()) {
                Optional<Input> in = inResult.stream().filter(input -> input.id.equals(id)).findFirst();
                Optional<Output> out = outResult.stream().filter(output -> output.id.equals(id)).findFirst();
                int j = i;
                in.ifPresent(input -> inMB.put(j, input));
                out.ifPresent(output -> outMB.put(j, output));
            }
        }

        inputMap = inMB;
        outputMap = outMB;
        return this;
    }
}