package mrtjp.projectred.gametests.synchronizer;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import com.gtnewhorizons.horizonqa.api.GameTestArguments;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.annotation.MethodSource;

import mrtjp.core.vec.Point;
import mrtjp.projectred.fabrication.AlloyWireICPart;
import mrtjp.projectred.fabrication.ICGateDefinition;
import mrtjp.projectred.fabrication.LeverICPart;
import mrtjp.projectred.fabrication.SequentialGateICPart;
import mrtjp.projectred.gametests.GateFixture;
import mrtjp.projectred.integration.GateDefinition;
import mrtjp.projectred.integration.SequentialGatePart;
import mrtjp.projectred.integration.Synchronizer;
import mrtjp.projectred.transmission.RedAlloyWirePart;

@GameTestHolder(
        value = "projectred",
        requiredMods = { "ProjRed|Integration", "ProjRed|Fabrication", "ProjRed|Transmission" })
public class SynchronizerFeedbackTests {

    public static List<GameTestArguments> implementations() {
        return Arrays.asList(GameTestArguments.named("world", false), GameTestArguments.named("ic", true));
    }

    @GameTest(template = "gate", timeoutTicks = 25)
    @MethodSource("implementations")
    public static void risingFeedbackSeesArmedPulseTermination(GameTestHelper helper, boolean ic) {
        SyncAccess sync = synchronizer(helper, ic);
        boolean[] reset = { false };
        boolean[] armed = { false };
        long[] terminationDelay = { -1 };
        sync.feedback = high -> {
            if (high && !reset[0]) {
                armed[0] = sync.pulsing.getAsBoolean();
                terminationDelay[0] = sync.scheduled.getAsLong() - helper.getWorld().getTotalWorldTime();
                reset[0] = true;
                sync.inputs.accept(14);
            }
        };
        helper.startSequence().thenExecute("trigger paired inputs", () -> sync.inputs.accept(10))
                .thenWaitUntil("output rise feeds reset back synchronously", 10, () -> helper.assertTrue(reset[0]))
                .thenExecute("feedback observed an armed pulse", () -> {
                    helper.assertTrue(armed[0], "pulse must be armed before output notification");
                    helper.assertEquals(2L, terminationDelay[0], "termination must be scheduled before feedback");
                })
                .thenWaitUntil(
                        "armed pulse terminates despite reentrant reset",
                        6,
                        () -> helper.assertEquals(0, sync.signal.getAsInt()))
                .thenExecute("pulse state cleared", () -> helper.assertFalse(sync.pulsing.getAsBoolean()))
                .thenSucceed();
    }

    @GameTest(template = "gate", timeoutTicks = 40)
    @MethodSource("implementations")
    public static void fallingFeedbackPreservesNextInputEvent(GameTestHelper helper, boolean ic) {
        SyncAccess sync = synchronizer(helper, ic);
        boolean[] fed = { false };
        sync.feedback = high -> {
            if (!high && !fed[0]) {
                fed[0] = true;
                sync.inputs.accept(2);
            }
        };
        helper.startSequence().thenExecute("trigger first pair", () -> sync.inputs.accept(10))
                .thenWaitUntil("first pulse reaches wire", 10, () -> helper.assertTrue(sync.signal.getAsInt() > 0))
                .thenExecute("release old inputs", () -> sync.inputs.accept(0))
                .thenWaitUntil("falling output feeds a new right edge back", 10, () -> {
                    helper.assertTrue(fed[0]);
                    helper.assertEquals(0, sync.signal.getAsInt());
                }).thenExecute("new input survives return from output notification", () -> {
                    helper.assertTrue(
                            sync.right.getAsBoolean(),
                            "new right event was erased after output notification");
                    helper.assertFalse(sync.left.getAsBoolean(), "old left event leaked into the next pair");
                    helper.assertFalse(sync.pulsing.getAsBoolean());
                    sync.inputs.accept(10);
                }).thenWaitUntil("new left edge completes a second pulse", 10, () -> {
                    helper.assertEquals(2, sync.rises);
                    helper.assertTrue(sync.signal.getAsInt() > 0);
                }).thenWaitUntil("second pulse terminates", 6, () -> helper.assertEquals(0, sync.signal.getAsInt()))
                .thenExecute("exactly two pulses", () -> helper.assertEquals(2, sync.rises)).thenSucceed();
    }

    private static SyncAccess synchronizer(GameTestHelper helper, boolean ic) {
        SyncAccess access = new SyncAccess();
        if (ic) {
            SequentialGateICPart gate = GateFixture.ic(helper, ICGateDefinition.Synchronizer());
            mrtjp.projectred.fabrication.Synchronizer logic = (mrtjp.projectred.fabrication.Synchronizer) gate
                    .getLogic();
            LeverICPart[] inputs = new LeverICPart[4];
            for (int r = 1; r < 4; r++) inputs[r] = GateFixture.input(gate, r);
            access.inputs = mask -> {
                for (int r = 1; r < 4; r++) {
                    if (inputs[r].on() != ((mask & (1 << r)) != 0)) inputs[r].readClientPacket(null);
                }
            };
            access.pulsing = logic::pulsing;
            access.right = logic::right;
            access.left = logic::left;
            access.scheduled = gate::schedTime;
            AlloyWireICPart wire = new AlloyWireICPart() {

                private boolean previous;

                @Override
                public void onNeighborChanged() {
                    super.onNeighborChanged();
                    boolean high = getSignal() > 0;
                    if (high != previous) {
                        previous = high;
                        access.transition(high);
                    }
                }
            };
            gate.world().setPart(new Point(gate.x(), gate.y()).offset(gate.toAbsolute(0)), wire);
            access.signal = wire::getSignal;
        } else {
            SequentialGatePart gate = (SequentialGatePart) GateFixture.world(helper, GateDefinition.Synchronizer());
            Synchronizer logic = (Synchronizer) gate.getLogicSequential();
            int[] current = { 0 };
            access.inputs = mask -> {
                for (int r = 1; r < 4; r++) {
                    int bit = 1 << r;
                    if ((current[0] & bit) != (mask & bit)) {
                        current[0] ^= bit;
                        helper.setRedstoneInput("port" + r, (mask & bit) != 0 ? 15 : 0);
                    }
                }
            };
            access.pulsing = logic::pulsing;
            access.right = logic::right;
            access.left = logic::left;
            access.scheduled = gate::schedTime;
            RedAlloyWirePart wire = new RedAlloyWirePart() {

                private boolean previous;

                @Override
                public void onNeighborChanged() {
                    super.onNeighborChanged();
                    boolean high = getSignal() > 0;
                    if (high != previous) {
                        previous = high;
                        access.transition(high);
                    }
                }
            };
            wire.preparePlacement(1, 0);
            GateFixture.add(helper, "port0", wire);
            access.signal = wire::getSignal;
        }
        return access;
    }

    private static class SyncAccess {

        IntConsumer inputs;
        IntSupplier signal;
        BooleanSupplier pulsing;
        BooleanSupplier right;
        BooleanSupplier left;
        LongSupplier scheduled;
        Consumer<Boolean> feedback = high -> {};
        int rises;

        void transition(boolean high) {
            if (high) rises++;
            feedback.accept(high);
        }
    }
}
