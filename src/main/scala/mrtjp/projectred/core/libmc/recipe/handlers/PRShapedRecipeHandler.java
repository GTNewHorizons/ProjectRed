package mrtjp.projectred.core.libmc.recipe.handlers;

import codechicken.nei.NEIClientUtils;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import mrtjp.projectred.core.libmc.recipe.recipes.ShapedBuilderRecipe;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

public class PRShapedRecipeHandler extends TemplateRecipeHandler {
    class CachedShapedRecipe extends CachedRecipe {
        List<PositionedStack> inputs;
        PositionedStack outputs;
        CachedShapedRecipe(ShapedBuilderRecipe r) {
            inputs = new ArrayList<>();
            outputs = new PositionedStack(r.getRecipeOutput(), 119, 24);
            for (int x = 0; x < r.builder.size; x++) {
                for (int y = 0; y < r.builder.size; y++) {
                    int idx = y * r.builder.size + x;
                    if (r.builder.inputMap.containsKey(idx)) {
                        PositionedStack stack = new PositionedStack(
                                r.builder.inputMap.get(idx).matchingInputs().toArray(),
                                25 + x * 18,
                                6 + y * 18,
                                false
                        );
                        stack.setMaxSize(1);
                        inputs.add(stack);
                    }
                }
            }
        }



        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, inputs);
        }

        @Override
        public PositionedStack getResult() {
            return outputs;
        }

        public void computeVisuals() {
            for (PositionedStack s : inputs) {
                s.generatePermutations();
            }
        }
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(
                new TemplateRecipeHandler.RecipeTransferRect(
                        new Rectangle(84, 23, 24, 18),
                        "crafting"
                )
        );
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (Object recipe : CraftingManager.getInstance().getRecipeList()) {
            IRecipe irecipe = (IRecipe) recipe;
            if (NEIServerUtils.areStacksSameTypeCrafting(irecipe.getRecipeOutput(), result)) {
                if (irecipe instanceof ShapedBuilderRecipe) {
                    CachedShapedRecipe cr = new CachedShapedRecipe((ShapedBuilderRecipe) irecipe);
                    cr.computeVisuals();
                    arecipes.add(cr);
                }
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (Object recipe : CraftingManager.getInstance().getRecipeList()) {
            IRecipe irecipe = (IRecipe) recipe;
            if (irecipe instanceof ShapedBuilderRecipe) {
                CachedShapedRecipe cr = new CachedShapedRecipe((ShapedBuilderRecipe) irecipe);
                if (cr.contains(cr.inputs, ingredient.getItem())) {
                    cr.computeVisuals();
                    if (cr.contains(cr.inputs, ingredient)) {
                        cr.setIngredientPermutation(cr.inputs, ingredient);
                        arecipes.add(cr);
                    }
                }
            }
        }
    }

    @Override
    public String getGuiTexture() {
        return "textures/gui/container/crafting_table.png";
    }

    @Override
    public String getRecipeName() {
        return NEIClientUtils.translate("recipe.shaped");
    }

    @Override
    public String getOverlayIdentifier() {
        return "crafting";
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiCrafting.class;
    }
}