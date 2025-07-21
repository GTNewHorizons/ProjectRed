package mrtjp.projectred.core.libmc.recipe.recipes;

import mrtjp.core.item.ItemKeyStack;
import mrtjp.projectred.core.libmc.recipe.item.Input;
import mrtjp.projectred.core.libmc.recipe.builders.ShapelessRecipeBuilder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShapelessBuilderRecipe implements IRecipe {
    public final ShapelessRecipeBuilder builder;

    public ShapelessBuilderRecipe(ShapelessRecipeBuilder builder) {
        this.builder = builder;
    }

    @Override
    public int getRecipeSize() {
        return builder.inResult.size();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return builder.outResult.get(0).createOutput();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return getRecipeOutput();
    }

    @Override
    public boolean matches(InventoryCrafting inv, World var2) {
        if (inv.getSizeInventory() < getRecipeSize()) return false;

        List<Input> required = builder.inResult;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemKeyStack stack = ItemKeyStack.get(inv.getStackInSlot(i));
            if (stack != null) {
                Optional<Input> match = required.stream().filter(e -> e.matches(stack)).findFirst();
                if (match.isPresent()) {
                    Input e = match.get();
                    int idx = required.indexOf(e);
                    required = new ArrayList<>(required.subList(0, idx));
                    required.addAll(required.subList(idx + 1, required.size()));
                } else {
                    return false;
                }
            }
        }
        return required.isEmpty();
    }
}