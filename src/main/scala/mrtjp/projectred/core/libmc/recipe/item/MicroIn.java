package mrtjp.projectred.core.libmc.recipe.item;

import codechicken.microblock.CornerMicroClass;
import codechicken.microblock.EdgeMicroClass;
import codechicken.microblock.FacePlacement;
import codechicken.microblock.HollowMicroClass;
import codechicken.microblock.ItemMicroPart;
import mrtjp.core.item.ItemKeyStack;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MicroIn extends Input {
    // class ids
    public static int face() {
        return FacePlacement.microClass().getClassId();
    }

    public static int hollowFace() {
        return HollowMicroClass.getClassId();
    }

    public static int corner() {
        return CornerMicroClass.getClassId();
    }

    public static int edge() {
        return EdgeMicroClass.getClassId();
    }

    // sizes
    public static final int eight = 1;
    public static final int fourth = 2;
    public static final int half = 4;

    private final int damage;
    private final ItemKeyStack sample;
    private final List<ItemStack> ins;

    public MicroIn(int classID, int size, String material) {
        this.damage = (classID << 8) | (size & 0xff);
        this.sample = ItemKeyStack.get(ItemMicroPart.create(damage, material));
        this.ins = new ArrayList<>();
        this.ins.add(sample.makeStack());
    }

    public MicroIn(int c, int s, Block b) {
        this(c, s, Block.blockRegistry.getNameForObject(b));
    }

    @Override
    public boolean matches(ItemKeyStack that) {
        return that.equals(sample);
    }

    @Override
    public List<ItemStack> matchingInputs() {
        return ins;
    }
}