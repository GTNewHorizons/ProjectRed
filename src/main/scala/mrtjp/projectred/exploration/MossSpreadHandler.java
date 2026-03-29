package mrtjp.projectred.exploration;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class MossSpreadHandler {

    public static void tickMossyCobble(World w, int x, int y, int z, Random rand) {
        doMossSpread(w, x, y, z, rand);
    }

    public static void tickStoneBricks(World w, int x, int y, int z, Random rand) {
        final int meta = w.getBlockMetadata(x, y, z);
        if (meta == 0) {
            crackFromHeat(w, x, y, z, rand);
        } else if (meta == 1) {
            doMossSpread(w, x, y, z, rand);
        }
    }

    private static void crackFromHeat(World w, int x, int y, int z, Random rand) {
        if (rand.nextInt(3) == 0) {
            if (w.checkChunksExist(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1)) {
                if (isBlockWet(w, x, y, z) && isBlockHot(w, x, y, z)) {
                    w.setBlock(x, y, z, Blocks.stonebrick, 2, 3);
                }
            }
        }
    }

    private static void doMossSpread(World w, int x, int y, int z, Random rand) {
        if (!w.checkChunksExist(x - 2, y - 2, z - 2, x + 2, y + 2, z + 2)) {
            return;
        }
        if (w.canBlockSeeTheSky(x, y + 1, z) || !isBlockTouchingAir(w, x, y, z)) {
            return;
        }
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            final int x2 = x + dir.offsetX;
            final int y2 = y + dir.offsetY;
            final int z2 = z + dir.offsetZ;
            if (w.canBlockSeeTheSky(x2, y2, z2)) continue;
            final Block block = w.getBlock(x2, y2, z2);
            final int meta = w.getBlockMetadata(x2, y2, z2);
            if (block == Blocks.cobblestone) {
                if (rand.nextInt(3) == 0 && isBlockWet(w, x2, y2, z2) && isBlockTouchingAir(w, x2, y2, z2)) {
                    w.setBlock(x2, y2, z2, Blocks.mossy_cobblestone, 0, 3);
                }
            } else if (block == Blocks.stonebrick && meta == 2) {
                if (rand.nextInt(3) == 0 && isBlockWet(w, x2, y2, z2) && isBlockTouchingAir(w, x2, y2, z2)) {
                    w.setBlock(x2, y2, z2, Blocks.stonebrick, 1, 3);
                }
            }
        }
    }

    private static boolean isBlockTouchingAir(World w, int x, int y, int z) {
        return checkAround(w, x, y, z, b -> b == Blocks.air);
    }

    private static boolean isBlockWet(World w, int x, int y, int z) {
        return checkAround(w, x, y, z, b -> b == Blocks.flowing_water || b == Blocks.water);
    }

    private static boolean isBlockHot(World w, int x, int y, int z) {
        return checkAround(w, x, y, z, b -> b == Blocks.flowing_lava || b == Blocks.lava);
    }

    private static boolean checkAround(World w, int x, int y, int z, BlockPredicate predicate) {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            final int x2 = x + dir.offsetX;
            final int y2 = y + dir.offsetY;
            final int z2 = z + dir.offsetZ;
            final Block block = w.getBlock(x2, y2, z2);
            if (predicate.test(block)) return true;
        }
        return false;
    }

    @FunctionalInterface
    private interface BlockPredicate {

        boolean test(Block b);
    }
}
