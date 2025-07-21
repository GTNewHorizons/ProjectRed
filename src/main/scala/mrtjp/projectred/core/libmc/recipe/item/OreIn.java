package mrtjp.projectred.core.libmc.recipe.item;

import mrtjp.core.item.ItemKeyStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static net.minecraftforge.oredict.OreDictionary.getOreID;
import static net.minecraftforge.oredict.OreDictionary.getOreIDs;
import static net.minecraftforge.oredict.OreDictionary.getOreName;
import static net.minecraftforge.oredict.OreDictionary.getOres;

public class OreIn extends Input {
    private List<Integer> oreIDs;
    private List<ItemStack> ins;

    public OreIn(List<Integer> oreIDs) {
        this.oreIDs = oreIDs;

        ins = new ArrayList<>();
        for (Integer oreID : oreIDs) {
            ins.addAll(getOres(getOreName(oreID)));
        }
    }

    public OreIn(int id) {
        this(Arrays.asList(id));
    }

    public OreIn(String name) {
        this(getOreID(name));
    }

    public OreIn(ItemStack stack) {
        this(oreIdsFromStack(stack));
    }

    private static List<Integer> oreIdsFromStack(ItemStack stack){
        List<Integer> list = new ArrayList<>();
        int[] ids = getOreIDs(stack);
        for (int i=0; i<ids.length; i++) {
            list.add(ids[i]);
        }
        return list;
    }

    public OreIn(Block b) {
        this(new ItemStack(b));
    }

    public OreIn(Item i) {
        this(new ItemStack(i));
    }

    public OreIn(ItemKeyStack stack) {
        this(stack.makeStack());
    }

    public boolean matches(ItemKeyStack that) {
        for (Integer oreID : getOreIDs(that.makeStack())) {
            if (oreIDs.contains(oreID)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public List<ItemStack> matchingInputs() {
        return ins;
    }
}

