package mrtjp.projectred.gametests.synchronizer;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

import com.gtnewhorizons.horizonqa.api.GameTestArguments;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TickCallbackHandle;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.annotation.MethodSource;

import mrtjp.core.vec.Point;
import mrtjp.projectred.fabrication.ICGateDefinition;
import mrtjp.projectred.fabrication.LeverICPart;
import mrtjp.projectred.fabrication.SequentialGateICPart;
import mrtjp.projectred.fabrication.TorchICPart;
import mrtjp.projectred.gametests.GateFixture;
import mrtjp.projectred.integration.GateDefinition;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Integration", "ProjRed|Fabrication" })
public class SynchronizerRegressionTests {

    public static List<GameTestArguments> implementations() {
        return Arrays.asList(GameTestArguments.named("world", false), GameTestArguments.named("ic", true));
    }

    @GameTest(template = "gate", timeoutTicks = 55)
    @MethodSource("implementations")
    public static void resetDuringPulseStillTerminatesAndRearms(GameTestHelper helper, boolean ic) {
        Runnable pair;
        Runnable reset;
        Runnable release;
        IntSupplier signal;
        if (ic) {
            SequentialGateICPart gate = GateFixture.ic(helper, ICGateDefinition.Synchronizer());
            LeverICPart right = GateFixture.input(gate, 1);
            LeverICPart left = GateFixture.input(gate, 3);
            LeverICPart clear = GateFixture.input(gate, 2);
            pair = () -> {
                right.readClientPacket(null);
                left.readClientPacket(null);
            };
            reset = () -> clear.readClientPacket(null);
            release = () -> {
                right.readClientPacket(null);
                left.readClientPacket(null);
                clear.readClientPacket(null);
            };
            signal = GateFixture.output(gate)::getSignal;
        } else {
            GateFixture.world(helper, GateDefinition.Synchronizer());
            pair = () -> {
                helper.setRedstoneInput("port1", 15);
                helper.setRedstoneInput("port3", 15);
            };
            reset = () -> helper.setRedstoneInput("port2", 15);
            release = () -> {
                helper.setRedstoneInput("port1", 0);
                helper.setRedstoneInput("port3", 0);
                helper.setRedstoneInput("port2", 0);
            };
            signal = GateFixture.output(helper)::getSignal;
        }
        int[] rises = { 0 };
        boolean[] previous = { false };
        helper.onEachTick("observe real output pulses", () -> {
            boolean high = signal.getAsInt() > 0;
            if (high && !previous[0]) rises[0]++;
            previous[0] = high;
            helper.assertTrue(rises[0] <= 2, "synchronizer emitted an extra pulse");
        });
        TickCallbackHandle heldReset = helper.onEachTickDisabled(
                "held reset cannot strand output high",
                () -> helper.assertEquals(0, signal.getAsInt(), "reset left output powered"));
        helper.startSequence().thenExecute("trigger first pair", pair)
                .thenWaitUntil("first output pulse is active", 10, () -> helper.assertTrue(signal.getAsInt() > 0))
                .thenExecute("reset during active pulse", reset)
                .thenWaitUntil("active pulse terminates normally", 10, () -> helper.assertEquals(0, signal.getAsInt()))
                .thenExecute("observe reset held", heldReset::enable).thenIdle(5)
                .thenExecute("release and re-arm inputs", () -> {
                    heldReset.disable();
                    release.run();
                }).thenExecute("trigger second pair", pair)
                .thenWaitUntil("fresh output pulse reaches wire", 10, () -> helper.assertTrue(signal.getAsInt() > 0))
                .thenWaitUntil("second pulse terminates", 10, () -> helper.assertEquals(0, signal.getAsInt()))
                .thenExecute("exactly two events were emitted", () -> helper.assertEquals(2, rises[0]))
                .thenExecute("observe final low", heldReset::enable).thenIdle(5).thenSucceed();
    }

    @GameTest(template = "gate", timeoutTicks = 20)
    public static void pendingIcInputAndReleaseRequestPersistence(GameTestHelper helper) {
        SequentialGateICPart gate = GateFixture.ic(helper, ICGateDefinition.Synchronizer());
        mrtjp.projectred.fabrication.Synchronizer logic = (mrtjp.projectred.fabrication.Synchronizer) gate.getLogic();
        Point right = new Point(gate.x(), gate.y()).offset(gate.toAbsolute(1));
        IntSupplier output = GateFixture.output(gate)::getSignal;
        // Torch placement/removal isolates the gate's save request from lever state changes.
        GateFixture.assertSaveRequested(helper, "workbench", () -> gate.world().setPart(right, new TorchICPart()));
        helper.assertTrue(logic.right(), "one input should remain latched awaiting the other");
        helper.assertFalse(logic.left(), "second input must not be latched");
        helper.assertEquals(0, output.getAsInt(), "one input must not produce an output pulse");
        GateFixture.assertSaveRequested(helper, "workbench", () -> gate.world().removePart(right));
        helper.assertTrue(logic.right(), "release must preserve the pending input latch");
        helper.succeed();
    }
}
