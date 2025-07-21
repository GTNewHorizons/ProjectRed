package mrtjp.projectred.core.libmc.recipe.handlers;

import codechicken.nei.NEIClientUtils;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import mrtjp.projectred.core.libmc.recipe.item.Input;
import mrtjp.projectred.core.libmc.recipe.recipes.ShapelessBuilderRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.util.ArrayList;
import java.util.List;

public class PRShapelessRecipeHandler extends PRShapedRecipeHandler {
    class CachedShapelessRecipe extends CachedRecipe {
        int[][] stackorder = new int[][]{
                {0, 0},
                {1, 0},
                {0, 1},
                {1, 1},
                {0, 2},
                {1, 2},
                {2, 0},
                {2, 1},
                {2, 2}
        };

        public List<PositionedStack> inputs;
        public PositionedStack output;
        CachedShapelessRecipe(ShapelessBuilderRecipe r) {
            inputs = new ArrayList<>();
            output = new PositionedStack(r.getRecipeOutput(), 119, 24);
            for (int i = 0; i < r.builder.inResult.size(); i++) {
                Input in = r.builder.inResult.get(i);
                PositionedStack stack = new PositionedStack(
                        in.matchingInputs().toArray(),
                        25 + stackorder[i][0] * 18,
                        6 + stackorder[i][1] * 18
                );
                stack.setMaxSize(1);
                inputs.add(stack);
            }
        }



        @Override
        public PositionedStack getResult() {
            return output;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, inputs);
        }
    }

    @Override
    public String getRecipeName() {
        return NEIClientUtils.translate("recipe.shapeless");
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (Object recipeObject : CraftingManager.getInstance().getRecipeList()) {
            IRecipe irecipe = (IRecipe) recipeObject;
            if (irecipe instanceof ShapelessBuilderRecipe) {
                if (NEIServerUtils.areStacksSameTypeCrafting(irecipe.getRecipeOutput(), result)) {
                    CachedShapelessRecipe recipe = new CachedShapelessRecipe((ShapelessBuilderRecipe) irecipe);
                    arecipes.add(recipe);
                }
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (Object recipeObject : CraftingManager.getInstance().getRecipeList()) {
            IRecipe irecipe = (IRecipe) recipeObject;
            if (irecipe instanceof ShapelessBuilderRecipe) {
                CachedShapelessRecipe recipe = new CachedShapelessRecipe((ShapelessBuilderRecipe) irecipe);
                if (recipe.contains(recipe.inputs, ingredient)) {
                    recipe.setIngredientPermutation(recipe.inputs, ingredient);
                    arecipes.add(recipe);
                }
            }
        }
    }
}