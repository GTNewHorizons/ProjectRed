package mrtjp.projectred.core.libmc.recipe.recipes;

import mrtjp.core.item.ItemKeyStack;
import mrtjp.projectred.core.libmc.recipe.item.Input;
import mrtjp.projectred.core.libmc.recipe.builders.ShapedRecipeBuilder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class ShapedBuilderRecipe implements IRecipe {
    public final ShapedRecipeBuilder builder;

    public ShapedBuilderRecipe(ShapedRecipeBuilder builder) {
        this.builder = builder;
    }

    @Override
    public int getRecipeSize() {
        return builder.map.length();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return getRecipeOutput();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return builder.outResult.get(0).createOutput();
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        if (inv.getSizeInventory() < getRecipeSize()) return false;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            Input in = builder.inputMap.getOrDefault(i, null);
            ItemKeyStack slot = ItemKeyStack.get(inv.getStackInSlot(i));
            if ((in == null && slot != null) ||
                    (slot == null && in != null) ||
                    (slot != null && in != null && !in.matches(slot))) {
                return false;
            }
        }
        return true;
    }
}