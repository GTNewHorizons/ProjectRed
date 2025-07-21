package mrtjp.projectred.core.libmc.recipe.item;

import mrtjp.core.item.ItemKeyStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemOut extends Output {
    private final ItemKeyStack key;

    public ItemOut(ItemKeyStack key) {
        this.key = key;
    }

    public ItemOut(ItemStack s) {
        this(ItemKeyStack.get(s));
    }

    public ItemOut(Block b) {
        this(new ItemStack(b));
    }

    public ItemOut(Item i) {
        this(new ItemStack(i));
    }

    @Override
    public boolean matches(ItemKeyStack that) {
        return key.equals(that);
    }

    @Override
    public ItemStack createOutput() {
        return key.makeStack();
    }
}
