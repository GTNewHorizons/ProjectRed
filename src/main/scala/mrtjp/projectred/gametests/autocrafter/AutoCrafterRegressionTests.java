package mrtjp.projectred.gametests.autocrafter;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import mrtjp.projectred.ProjectRedExpansion;
import mrtjp.projectred.expansion.ItemPlan;
import mrtjp.projectred.expansion.TileAutoCrafter;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Expansion" })
public class AutoCrafterRegressionTests {

    // Without power the bench attempts one craft every 40 ticks.
    private static final int CRAFT_WINDOW = 100;
    private static final int FIRST_STORAGE_SLOT = 9;

    /** Plan made with oak planks, bench stocked with spruce planks: the stick recipe is an ore recipe. */
    @GameTest(template = "autocrafter", timeoutTicks = CRAFT_WINDOW)
    public static void oreRecipeAcceptsSubstituteIngredient(GameTestHelper helper) {
        TileAutoCrafter bench = bench(helper);
        bench.setInventorySlotContents(FIRST_STORAGE_SLOT, new ItemStack(Blocks.planks, 2, 1));
        helper.startSequence().thenIdle(1)
                .thenExecute(
                        "stick plan resolves to an ore recipe",
                        () -> helper.assertTrue(bench.currentRecipe() instanceof ShapedOreRecipe))
                .thenWaitUntil("sticks are crafted", () -> helper.assertEquals(4, count(bench, Items.stick)))
                .thenExecute(
                        "planks are consumed",
                        () -> helper.assertEquals(0, count(bench, Item.getItemFromBlock(Blocks.planks))))
                .thenSucceed();
    }

    /**
     * Same item as the plan but with different NBT. Mirrors GT crafting tools, which keep durability in NBT and so stop
     * matching the plan after their first use.
     */
    @GameTest(template = "autocrafter", timeoutTicks = CRAFT_WINDOW)
    public static void oreRecipeAcceptsIngredientWithChangedNbt(GameTestHelper helper) {
        TileAutoCrafter bench = bench(helper);
        ItemStack worn = new ItemStack(Blocks.planks, 2, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Damage", 400);
        worn.setTagCompound(tag);
        bench.setInventorySlotContents(FIRST_STORAGE_SLOT, worn);
        helper.succeedWhen(() -> count(bench, Items.stick) == 4);
    }

    /** Ingredients that do not satisfy the recipe must never be consumed. */
    @GameTest(template = "autocrafter", timeoutTicks = CRAFT_WINDOW)
    public static void unrelatedIngredientIsNotCrafted(GameTestHelper helper) {
        TileAutoCrafter bench = bench(helper);
        bench.setInventorySlotContents(FIRST_STORAGE_SLOT, new ItemStack(Blocks.cobblestone, 2));
        helper.onEachTick("nothing is crafted", () -> {
            helper.assertEquals(0, count(bench, Items.stick));
            helper.assertEquals(2, count(bench, Item.getItemFromBlock(Blocks.cobblestone)));
        });
        helper.succeedAtTimeout();
    }

    /** Places an auto crafter holding a plan for oak planks -> 4 sticks. */
    private static TileAutoCrafter bench(GameTestHelper helper) {
        helper.setBlock("bench", ProjectRedExpansion.machine2(), 11);
        TestPos p = helper.absolute("bench");
        TileAutoCrafter bench = (TileAutoCrafter) helper.getWorld().getTileEntity(p.x(), p.y(), p.z());

        ItemStack[] inputs = new ItemStack[9];
        inputs[0] = new ItemStack(Blocks.planks, 1, 0);
        inputs[3] = new ItemStack(Blocks.planks, 1, 0);
        ItemStack plan = new ItemStack(ProjectRedExpansion.itemPlan());
        ItemPlan.savePlan(plan, inputs, new ItemStack(Items.stick, 4));
        bench.setInventorySlotContents(0, plan);
        return bench;
    }

    private static int count(TileAutoCrafter bench, Item item) {
        int total = 0;
        for (int i = FIRST_STORAGE_SLOT; i < bench.getSizeInventory(); i++) {
            ItemStack s = bench.getStackInSlot(i);
            if (s != null && s.getItem() == item) total += s.stackSize;
        }
        return total;
    }
}
