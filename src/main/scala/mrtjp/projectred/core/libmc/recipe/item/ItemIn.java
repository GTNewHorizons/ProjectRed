package mrtjp.projectred.core.libmc.recipe.item;

import mrtjp.core.item.ItemKeyStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.List;

public class ItemIn extends Input {
    private ItemKeyStack key;
    private boolean nbt = true;

    public ItemIn(ItemKeyStack key) {
        this.key = key;
    }

    public ItemIn(ItemStack s) {
        this(ItemKeyStack.get(s));
    }

    public ItemIn(Block b) {
        this(new ItemStack(b));
    }

    public ItemIn(Item i) {
        this(new ItemStack(i));
    }

    public ItemIn matchNBT(boolean flag) {
        this.nbt = flag;
        return this;
    }

    @Override
    public boolean matches(ItemKeyStack that) {
        return key.key().item().equals(that.key().item()) &&
                (!nbt || key.key().tag().equals(that.key().tag())) &&
                (key.key().itemDamage() == OreDictionary.WILDCARD_VALUE || that.key().itemDamage() == OreDictionary.WILDCARD_VALUE ||
                        key.key().itemDamage() == that.key().itemDamage());
    }


    public List<ItemStack> matchingInputs() {
        return Collections.singletonList(key.makeStack());
    }
}