package mrtjp.projectred.core.libmc.recipe.item;

import net.minecraft.item.ItemStack;

public abstract class Output extends TRecipeObject {
    public abstract ItemStack createOutput();
}