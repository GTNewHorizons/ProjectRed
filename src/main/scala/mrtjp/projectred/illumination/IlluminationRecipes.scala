package mrtjp.projectred.illumination

import java.lang.{Character => JC}

import cpw.mods.fml.common.registry.GameRegistry
import mrtjp.projectred.ProjectRedIllumination
import mrtjp.projectred.core.PartDefs
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.ShapedOreRecipe

object IlluminationRecipes {
  def initRecipes() {
    initLighting()
  }

  private def initLighting() {

    /** Lamps * */
    for (i <- 0 until 16) {
      GameRegistry.addRecipe(
        new ShapedOreRecipe(
          new ItemStack(ProjectRedIllumination.blockLamp, 1, i),
          "gIg",
          "gIg",
          "gtg",
          'g': JC,
          "paneGlassColorless",
          'I': JC,
          PartDefs.ILLUMARS.toSeq(i).makeStack,
          't': JC,
          "dustRedstone"
        )
      )

      GameRegistry.addRecipe(
        new ShapedOreRecipe(
          new ItemStack(ProjectRedIllumination.blockLamp, 1, i + 16),
          "gIg",
          "gIg",
          "gtg",
          'g': JC,
          "paneGlassColorless",
          'I': JC,
          PartDefs.ILLUMARS.toSeq(i).makeStack,
          't': JC,
          Blocks.redstone_torch
        )
      )
    }

    /** Lanterns * */
    for (i <- 0 until 16) {
      GameRegistry.addRecipe(
        new ShapedOreRecipe(
          LightObjLantern.makeStack(i),
          "PNP",
          "GIG",
          "PRP",
          'P': JC,
          PartDefs.PLATE.makeStack,
          'N': JC,
          "nuggetGold",
          'G': JC,
          "paneGlassColorless",
          'I': JC,
          PartDefs.ILLUMARS.toSeq(i).makeStack,
          'R': JC,
          "dustRedstone"
        )
      )
      GameRegistry.addRecipe(
        new ShapedOreRecipe(
          LightObjLantern.makeInvStack(i),
          "PNP",
          "GIG",
          "PRP",
          'P': JC,
          PartDefs.PLATE.makeStack,
          'N': JC,
          "nuggetGold",
          'G': JC,
          "paneGlassColorless",
          'I': JC,
          PartDefs.ILLUMARS.toSeq(i).makeStack,
          'R': JC,
          Blocks.redstone_torch
        )
      )
    }

    /** Buttons * */
    for (i <- 0 until 16) {
      GameRegistry.addShapelessRecipe(
        new ItemStack(ProjectRedIllumination.itemPartIllumarButton, 1, i),
        Blocks.stone_button,
        PartDefs.ILLUMARS.toSeq(i).makeStack,
        PartDefs.ILLUMARS.toSeq(i).makeStack
      )
      GameRegistry.addShapelessRecipe(
        new ItemStack(ProjectRedIllumination.itemPartIllumarFButton, 1, i),
        new ItemStack(ProjectRedIllumination.itemPartIllumarButton, 1, i),
        Blocks.redstone_torch
      )
    }

    /** Fallout Lights * */
    for (i <- 0 until 16) {
      GameRegistry.addRecipe(
        new ShapedOreRecipe(
          LightObjFallout.makeStack(i),
          "CCC",
          "CIC",
          "NPN",
          'C': JC,
          Blocks.iron_bars,
          'I': JC,
          PartDefs.ILLUMARS.toSeq(i).makeStack,
          'N': JC,
          "nuggetGold",
          'P': JC,
          PartDefs.CONDUCTIVEPLATE.makeStack
        )
      )

      GameRegistry.addRecipe(
        new ShapedOreRecipe(
          LightObjFallout.makeInvStack(i),
          "CCC",
          "CIC",
          "NPN",
          'C': JC,
          Blocks.iron_bars,
          'I': JC,
          PartDefs.ILLUMARS.toSeq(i).makeStack,
          'N': JC,
          "nuggetGold",
          'P': JC,
          PartDefs.CATHODE.makeStack
        )
      )
    }

    /** Cage Lamps * */
    for (i <- 0 until 16) {
      GameRegistry.addRecipe(
        LightObjCage.makeStack(i),
        " C ",
        "CIC",
        "pPp",
        'C': JC,
        Blocks.iron_bars,
        'I': JC,
        PartDefs.ILLUMARS.toSeq(i).makeStack,
        'p': JC,
        PartDefs.PLATE.makeStack,
        'P': JC,
        PartDefs.CONDUCTIVEPLATE.makeStack
      )

      GameRegistry.addRecipe(
        LightObjCage.makeInvStack(i),
        " C ",
        "CIC",
        "pPp",
        'C': JC,
        Blocks.iron_bars,
        'I': JC,
        PartDefs.ILLUMARS.toSeq(i).makeStack,
        'p': JC,
        PartDefs.PLATE.makeStack,
        'P': JC,
        PartDefs.CATHODE.makeStack
      )
    }

    /** Fixtures * */
    for (i <- 0 until 16) {
      GameRegistry.addRecipe(
        new ShapedOreRecipe(
          LightObjFixture.makeStack(i),
          "ggg",
          "gIg",
          "pPp",
          'g': JC,
          "paneGlassColorless",
          'I': JC,
          PartDefs.ILLUMARS.toSeq(i).makeStack,
          'p': JC,
          PartDefs.PLATE.makeStack,
          'P': JC,
          PartDefs.CONDUCTIVEPLATE.makeStack
        )
      )
      GameRegistry.addRecipe(
        new ShapedOreRecipe(
          LightObjFixture.makeInvStack(i),
          "ggg",
          "gIg",
          "pPp",
          'g': JC,
          "paneGlassColorless",
          'I': JC,
          PartDefs.ILLUMARS.toSeq(i).makeStack,
          'p': JC,
          PartDefs.PLATE.makeStack,
          'P': JC,
          PartDefs.CATHODE.makeStack
        )
      )
    }
  }
}
