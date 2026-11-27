package mrtjp.projectred.gametests.lamp;

import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import mrtjp.projectred.ProjectRedIllumination;
import mrtjp.projectred.illumination.BlockLamp;
import mrtjp.projectred.illumination.TileLamp;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Illumination" })
public class LampRegressionTests {

    @GameTest(template = "gate", timeoutTicks = 40)
    public static void litLampKeepsEmittingAfterCacheEntryIsCleared(GameTestHelper helper) {
        helper.setBlock("gate", ProjectRedIllumination.blockLamp(), 0);
        helper.setRedstoneInput("port0", 15);
        TileLamp lamp = helper.assertTileEntityPresent(TileLamp.class, "gate");
        TestPos p = helper.absolute("gate");
        World world = helper.getWorld();

        helper.startSequence().thenWaitUntil("powered lamp turns on and lights the block", 20, () -> {
            helper.assertTrue(lamp.isOn(), "lamp is powered on");
            helper.assertEquals(15, world.getSavedLightValue(EnumSkyBlock.Block, p.x(), p.y(), p.z()), "lit lamp");
        }).thenExecute("drop the cached entry, then re-light the lamp position", () -> {
            BlockLamp.clearLightValue(world, p.x(), p.y(), p.z());
            world.updateLightByType(EnumSkyBlock.Block, p.x(), p.y(), p.z());
            helper.assertEquals(
                    15,
                    world.getSavedLightValue(EnumSkyBlock.Block, p.x(), p.y(), p.z()),
                    "server block light after a cache miss");
        }).thenSucceed();
    }
}
