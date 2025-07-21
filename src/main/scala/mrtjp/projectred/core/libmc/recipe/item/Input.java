package mrtjp.projectred.core.libmc.recipe.item;

import net.minecraft.item.ItemStack;

import java.util.List;

public abstract class Input extends TRecipeObject {
    public abstract List<ItemStack> matchingInputs();
}
