package mrtjp.projectred.expansion

import java.util.{Map => JMap}
import mrtjp.core.item.ItemKeyStack
import mrtjp.projectred.core.libmc.recipe._
import mrtjp.projectred.core.libmc.recipe.recipes.InductiveFurnaceRecipe
import mrtjp.projectred.core.libmc.recipe.builders.InductiveFurnaceRecipeBuilder
import mrtjp.projectred.core.libmc.recipe.item.{ItemIn, ItemOut, OreIn}
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.{ItemFood, ItemStack}
import net.minecraftforge.oredict.OreDictionary

object InductiveFurnaceRecipeLib {
  var recipes = IndexedSeq[InductiveFurnaceRecipe]()

  def getRecipeFor(in: ItemStack): InductiveFurnaceRecipe = {
    val key = ItemKeyStack.get(in)
    for (r <- recipes) if (r.in.matches(key)) return r
    null
  }

  def getRecipeOf(out: ItemStack): InductiveFurnaceRecipe = {
    val key = ItemKeyStack.get(out)
    for (r <- recipes) if (r.out.matches(key)) return r
    null
  }

  def addRecipe(r: InductiveFurnaceRecipe) {
    recipes :+= r
  }

  def addRecipe(in: ItemStack, out: ItemStack, ticks: Int) {
    val b = new InductiveFurnaceRecipeBuilder()
    b.addInput( new ItemIn(in))
    b.addOutput( new ItemOut(out))
    b.setBurnTime(ticks)
    b.registerResult()
  }

  def addOreRecipe(in: ItemStack, out: ItemStack, ticks: Int) {
    val b = new InductiveFurnaceRecipeBuilder()
    b.addInput(new OreIn(in))
    b.addOutput(new ItemOut(out))
    b.setBurnTime(ticks)
    b.registerResult()
  }

  def addOreRecipe(in: String, out: ItemStack, ticks: Int) {
    val b = new InductiveFurnaceRecipeBuilder()
    b.addInput(new OreIn(in))
    b.addOutput(new ItemOut(out))
    b.setBurnTime(ticks)
    b.registerResult()
  }

  def init() {
    import scala.collection.JavaConversions._

    def isDust(stack: ItemStack) = getOreName(stack).startsWith("dust")
    def isIngot(stack: ItemStack) = getOreName(stack).startsWith("ingot")
    def getOreName(stack: ItemStack) = {
      val IDs = OreDictionary.getOreIDs(stack)
      if (IDs.isEmpty) "Unknown" else OreDictionary.getOreName(IDs(0))
    }

    val sl = FurnaceRecipes.smelting.getSmeltingList
      .asInstanceOf[JMap[ItemStack, ItemStack]]
    for ((in, out) <- sl) try {
      if (getRecipeFor(in) == null) {
        if (in.getItem.isInstanceOf[ItemFood]) addRecipe(in, out, 40)
        else if (isDust(in) && isIngot(out)) addOreRecipe(in, out, 80 * 10 / 16)
        else if (OreDictionary.getOreIDs(in).nonEmpty) addOreRecipe(in, out, 80)
        else addRecipe(in, out, 80)
      }
    } catch {
      case e: Exception =>
    }
  }
}