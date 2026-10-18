package mrtjp.projectred.gametests.randomizer;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import mrtjp.projectred.gametests.GateFixture;
import mrtjp.projectred.integration.BundledGatePart;
import mrtjp.projectred.integration.BusRandomizer;
import mrtjp.projectred.integration.GateDefinition;
import mrtjp.projectred.transmission.BundledCablePart;
import mrtjp.projectred.transmission.BundledCommons;
import mrtjp.projectred.transmission.WireDef;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Integration", "ProjRed|Transmission" })
public class BusRandomizerRegressionTests {

    @GameTest(template = "gate", timeoutTicks = 20)
    public static void pendingSelectionUsesRestoredChannelFifteen(GameTestHelper helper) {
        BundledGatePart gate = (BundledGatePart) GateFixture.world(helper, GateDefinition.BusRandomizer());
        BusRandomizer logic = gate.getLogic();
        BundledCablePart receiver = new BundledCablePart();
        receiver.preparePlacement(1, WireDef.BUNDLED_N().meta());
        GateFixture.add(helper, "port0", receiver);
        long[] deadline = { -1 };
        helper.startSequence()
                .thenWaitUntil("initial work has settled", 5, () -> helper.assertEquals(-1L, gate.schedTime()))
                .thenExecute("restore a pending selection restricted to channel fifteen", () -> {
                    NBTTagCompound saved = new NBTTagCompound();
                    gate.save(saved);
                    saved.setShort("in", (short) 0x8000);
                    saved.setShort("out", (short) 0);
                    saved.setByte("state", (byte) 2);
                    deadline[0] = helper.getWorld().getTotalWorldTime() + 1;
                    saved.setLong("schedTime", deadline[0]);
                    gate.load(saved);
                    logic.rand().setSeed(0);
                })
                .thenWaitUntil(
                        "restored deadline has been processed by the server",
                        3,
                        () -> helper.assertTrue(helper.getWorld().getTotalWorldTime() >= deadline[0]))
                .thenExecute(
                        "selection reaches the attached receiver",
                        () -> helper.assertEquals(
                                0x8000,
                                BundledCommons.packDigital(receiver.getBundledSignal()),
                                "channel fifteen is the only legal selection"))
                .thenSucceed();
    }
}
