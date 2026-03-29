package mrtjp.projectred.exploration.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import mrtjp.projectred.exploration.BlockMossyCobblestone;

@Mixin(value = Block.class)
public class BlockMixin {

    @Dynamic
    @Redirect(
            method = { "registerBlocks", "func_149671_p" },
            at = @At(value = "NEW", target = "(Lnet/minecraft/block/material/Material;)Lnet/minecraft/block/Block;"),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=mossy_cobblestone"),
                    to = @At(value = "CONSTANT", args = "stringValue=stoneMoss")),
            remap = false)
    private static Block replaceBaseMossyCobble(Material materialIn) {
        return new BlockMossyCobblestone(materialIn);
    }
}
