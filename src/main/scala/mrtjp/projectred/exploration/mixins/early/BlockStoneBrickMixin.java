package mrtjp.projectred.exploration.mixins.early;

import mrtjp.projectred.exploration.MossSpreadHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(BlockStoneBrick.class)
public class BlockStoneBrickMixin extends Block {

    protected BlockStoneBrickMixin(Material materialIn) {
        super(materialIn);
        throw new RuntimeException("Direct instantiation of BlockStoneBrickMixin is not allowed");
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void enableRandomTicking$ProjRed(CallbackInfo ci) {
        ((BlockStoneBrick) ((Object) this)).setTickRandomly(true);
    }

    @Override
    public void updateTick(World worldIn, int x, int y, int z, Random random) {
        MossSpreadHandler.onBlockUpdate(worldIn, x, y, z, this);
    }
}

