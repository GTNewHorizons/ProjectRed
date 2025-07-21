package mrtjp.projectred.core.libmc.recipe.item;

import mrtjp.core.item.ItemKeyStack;

public abstract class TRecipeObject {
    public String id = "";

    public TRecipeObject to(String i) {
        String newId = i.substring(0, 1);
        return this;
    }

    public abstract boolean matches(ItemKeyStack that);
}
