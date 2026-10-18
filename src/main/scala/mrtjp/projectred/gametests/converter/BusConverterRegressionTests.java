package mrtjp.projectred.gametests.converter;

import java.util.Arrays;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestArguments;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.annotation.MethodSource;

import mrtjp.projectred.gametests.GateFixture;
import mrtjp.projectred.integration.BundledGatePart;
import mrtjp.projectred.integration.GateDefinition;
import mrtjp.projectred.transmission.BundledCablePart;
import mrtjp.projectred.transmission.BundledCommons;
import mrtjp.projectred.transmission.RedAlloyWirePart;
import mrtjp.projectred.transmission.WireDef;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Integration", "ProjRed|Transmission" })
public class BusConverterRegressionTests {

    public static List<GameTestArguments> restorePaths() {
        return Arrays
                .asList(GameTestArguments.named("legacy", false), GameTestArguments.named("resaved-before-tick", true));
    }

    @GameTest(template = "gate", timeoutTicks = 30)
    public static void attenuatedWireSelectsCeilingAnalogLevel(GameTestHelper helper) {
        BundledGatePart gate = (BundledGatePart) GateFixture.world(helper, GateDefinition.BusConverter());
        BundledCablePart receiver = output(helper);
        RedAlloyWirePart near = new RedAlloyWirePart();
        near.preparePlacement(1, 0);
        GateFixture.add(helper, "port2", near);
        RedAlloyWirePart far = new RedAlloyWirePart();
        far.preparePlacement(1, 0);
        GateFixture.add(helper, "inputWireEnd", far);
        helper.startSequence()
                .thenExecute(
                        "power two real red-alloy wire segments",
                        () -> helper.setRedstoneInput("analogSource", 15))
                .thenWaitUntil(
                        "attenuated input reaches converter",
                        10,
                        () -> helper.assertEquals(254, gate.getRedstoneInput(2)))
                .thenWaitUntil(
                        "ceiling level reaches bundled receiver",
                        10,
                        () -> helper.assertEquals(0x8000, BundledCommons.packDigital(receiver.getBundledSignal())))
                .thenSucceed();
    }

    @GameTest(template = "gate", timeoutTicks = 25)
    @MethodSource("restorePaths")
    public static void truncatedLegacyOutputReconcilesWithoutInputEdge(GameTestHelper helper, boolean resave) {
        helper.setRedstoneInput("port2", 15);
        NBTTagCompound tag = legacy(15, -1);
        if (resave) {
            BundledGatePart original = loaded(tag);
            tag = new NBTTagCompound();
            original.save(tag);
        }
        GateFixture.world(helper, GateDefinition.BusConverter(), tag);
        BundledCablePart receiver = output(helper);
        helper.startSequence()
                .thenWaitUntil(
                        "loaded channel fifteen reaches receiver without an input edge",
                        10,
                        () -> helper.assertEquals(0x8000, BundledCommons.packDigital(receiver.getBundledSignal())))
                .thenExecute(
                        "observe reconciled output",
                        () -> helper.onEachTick(
                                "restored channel stays powered",
                                () -> helper
                                        .assertEquals(0x8000, BundledCommons.packDigital(receiver.getBundledSignal()))))
                .thenIdle(5).thenSucceed();
    }

    @GameTest(template = "gate", timeoutTicks = 25)
    public static void legacyDeadlineSamplesInputBeforePublishingOutput(GameTestHelper helper) {
        helper.setRedstoneInput("port2", 15);
        GateFixture.world(helper, GateDefinition.BusConverter(), legacy(0, 0));
        BundledCablePart receiver = output(helper);
        helper.assertEquals(0, receiver.getBundledSignal()[0] & 0xff, "load must not publish stale channel zero");
        helper.onEachTick(
                "no false channel-zero pulse",
                () -> helper.assertEquals(0, receiver.getBundledSignal()[0] & 0xff));
        helper.startSequence()
                .thenWaitUntil(
                        "freshly sampled channel fifteen reaches receiver",
                        10,
                        () -> helper.assertEquals(0x8000, BundledCommons.packDigital(receiver.getBundledSignal())))
                .thenSucceed();
    }

    @GameTest(template = "gate", timeoutTicks = 25)
    public static void zeroInputModeSwitchSelectsChannelZero(GameTestHelper helper) {
        NBTTagCompound tag = GateFixture.tag(GateDefinition.BusConverter());
        tag.setByte("shape", (byte) 1);
        tag.setShort("in0", (short) 0);
        tag.setShort("out0", (short) 0);
        tag.setLong("schedTime", -1);
        BundledGatePart gate = (BundledGatePart) GateFixture.world(helper, GateDefinition.BusConverter(), tag);
        BundledCablePart receiver = output(helper);
        helper.startSequence().thenExecute("switch from bundled input to analog input", gate::configure)
                .thenWaitUntil(
                        "zero analog input selects channel zero without an input edge",
                        10,
                        () -> helper.assertEquals(1, BundledCommons.packDigital(receiver.getBundledSignal())))
                .thenSucceed();
    }

    private static NBTTagCompound legacy(int input, long deadline) {
        NBTTagCompound tag = GateFixture.tag(GateDefinition.BusConverter());
        tag.setByte("in", (byte) input);
        tag.setByte("in0", (byte) 0);
        tag.setByte("out0", (byte) 0);
        tag.setLong("schedTime", deadline);
        return tag;
    }

    private static BundledGatePart loaded(NBTTagCompound tag) {
        BundledGatePart gate = new BundledGatePart();
        gate.load(tag);
        return gate;
    }

    private static BundledCablePart output(GameTestHelper helper) {
        BundledCablePart cable = new BundledCablePart();
        cable.preparePlacement(1, WireDef.BUNDLED_N().meta());
        GateFixture.add(helper, "port0", cable);
        return cable;
    }
}
