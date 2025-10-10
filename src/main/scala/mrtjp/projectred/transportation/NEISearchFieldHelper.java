package mrtjp.projectred.transportation;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import codechicken.nei.SearchField;
import codechicken.nei.api.ItemFilter;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class NEISearchFieldHelper {

    private static SearchField searchField = null;

    public NEISearchFieldHelper() {}

    protected static SearchField getSearchField() {
        if (searchField != null) {
            return searchField;
        }

        try {
            final Class<? super Object> clazz = ReflectionHelper
                    .getClass(NEISearchFieldHelper.class.getClassLoader(), "codechicken.nei.LayoutManager");
            final Field fldSearchField = clazz.getField("searchField");
            searchField = (SearchField) fldSearchField.get(clazz);
        } catch (Throwable __) {}

        return searchField;
    }

    public static boolean existsSearchField() {
        return getSearchField() != null;
    }

    public static Predicate<ItemStack> getFilter(String filterText) {
        final SearchField searchField = getSearchField();

        if (searchField != null) {
            final ItemFilter filter = searchField.getFilter(filterText);
            return filter::matches;
        }

        return null;
    }
}
