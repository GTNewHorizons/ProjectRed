package mrtjp.projectred.exploration;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class BlockMossyCobblestone extends Block {

    public BlockMossyCobblestone(Material materialIn) {
        super(Material.rock);
        this.setTickRandomly(true);
    }

    @Override
    public void updateTick(World worldIn, int x, int y, int z, Random random) {
        super.updateTick(worldIn, x, y, z, random);
        if (!worldIn.checkChunksExist(x, 0, z, x, 0, z)) {
            return;
        }
        MossSpreadHandler.onBlockUpdate(worldIn, x, y, z, this);
    }
}
