package mrtjp.projectred.exploration.mixins.early;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mrtjp.projectred.exploration.MossSpreadHandler;

@Mixin(BlockStoneBrick.class)
public class BlockStoneBrickMixin extends Block {

    protected BlockStoneBrickMixin(Material materialIn) {
        super(materialIn);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void enableRandomTicking(CallbackInfo ci) {
        this.setTickRandomly(true);
    }

    @Override
    public void updateTick(World worldIn, int x, int y, int z, Random random) {
        super.updateTick(worldIn, x, y, z, random);
        MossSpreadHandler.tickStoneBricks(worldIn, x, y, z, random);
    }
}
