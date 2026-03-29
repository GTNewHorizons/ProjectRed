package mrtjp.projectred.exploration;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import mrtjp.projectred.core.Configurator;

public class BlockMossyCobblestone extends Block {

    public BlockMossyCobblestone(Material materialIn) {
        super(materialIn);
        this.setTickRandomly(true);
    }

    @Override
    public void updateTick(World worldIn, int x, int y, int z, Random random) {
        super.updateTick(worldIn, x, y, z, random);
        if (!Configurator.gen_SpreadingMoss() || !worldIn.checkChunksExist(x - 1, 0, z - 1, x + 1, 0, z + 1)) {
            return;
        }
        MossSpreadHandler.onBlockUpdate(worldIn, x, y, z, this);
    }
}
