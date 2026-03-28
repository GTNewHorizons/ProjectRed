package mrtjp.projectred.exploration.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mrtjp.projectred.exploration.BlockMossyCobblestone;

@Mixin(value = Block.class)
public class BlockMixin {

    @WrapOperation(
            method = "registerBlocks",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/block/material/Material;)Lnet/minecraft/block/Block;",
                    ordinal = 3),
            remap = false,
            require = 1)
    private static Block replaceBaseMossyCobble$ProjRed(Material materialIn, Operation<Block> original) {
        return new BlockMossyCobblestone(materialIn);
    }
}
