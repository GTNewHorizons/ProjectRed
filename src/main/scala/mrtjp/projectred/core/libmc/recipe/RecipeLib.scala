package mrtjp.projectred.core.libmc.recipe

import mrtjp.projectred.core.libmc.recipe.recipes.ShapelessBuilderRecipe
import mrtjp.projectred.core.libmc.recipe.recipes.ShapedBuilderRecipe
import net.minecraftforge.oredict.RecipeSorter
import net.minecraftforge.oredict.RecipeSorter.Category._

object RecipeLib {

  def loadLib() {
    RecipeSorter.register(
      "projectred:shaped",
      classOf[ShapedBuilderRecipe],
      SHAPED,
      "after:forge:shaped"
    )
    RecipeSorter.register(
      "projectred:shapeless",
      classOf[ShapelessBuilderRecipe],
      SHAPELESS,
      "after:forge:shapeless"
    )
  }
}
