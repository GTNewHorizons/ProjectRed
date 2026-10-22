package mrtjp.projectred.gametests.counter;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import com.gtnewhorizons.horizonqa.api.GameTestArguments;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TickCallbackHandle;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.annotation.MethodSource;

import mrtjp.projectred.fabrication.ICGateDefinition;
import mrtjp.projectred.fabrication.LeverICPart;
import mrtjp.projectred.fabrication.SequentialGateICPart;
import mrtjp.projectred.gametests.GateFixture;
import mrtjp.projectred.integration.Counter;
import mrtjp.projectred.integration.GateDefinition;
import mrtjp.projectred.integration.SequentialGatePart;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Integration", "ProjRed|Fabrication" })
public class CounterRegressionTests {

    public static List<GameTestArguments> implementations() {
        return Arrays.asList(GameTestArguments.named("world", false), GameTestArguments.named("ic", true));
    }

    @GameTest(template = "gate", timeoutTicks = 65)
    @MethodSource("implementations")
    public static void maximumEditReconcilesReceiverWithoutChangingCount(GameTestHelper helper, boolean ic) {
        CounterAccess counter = counter(helper, ic);
        TickCallbackHandle stableCount = helper.onEachTickDisabled(
                "maximum edit preserves count",
                () -> helper.assertEquals(4, counter.count.getAsInt(), "configuration changed the stored count"));
        TickCallbackHandle staysLow = helper.onEachTickDisabled(
                "raised maximum keeps receiver unpowered",
                () -> helper.assertEquals(0, counter.signal.getAsInt(), "receiver regained maximum signal"));
        TickCallbackHandle staysHigh = helper.onEachTickDisabled(
                "restored maximum keeps receiver powered",
                () -> helper.assertTrue(counter.signal.getAsInt() > 0, "receiver lost maximum signal"));
        helper.startSequence().thenExecute("reach maximum with four input edges", () -> fill(helper, counter))
                .thenWaitUntil(
                        "maximum signal reaches attached wire",
                        15,
                        () -> helper.assertTrue(counter.signal.getAsInt() > 0))
                .thenExecute("raise maximum to five", () -> {
                    stableCount.enable();
                    counter.maximum.accept(5);
                })
                .thenWaitUntil(
                        "receiver clears without another input edge",
                        15,
                        () -> helper.assertEquals(0, counter.signal.getAsInt()))
                .thenExecute("observe cleared receiver", staysLow::enable).thenIdle(5)
                .thenExecute("restore maximum to four", () -> {
                    staysLow.disable();
                    counter.maximum.accept(4);
                })
                .thenWaitUntil(
                        "receiver regains maximum signal",
                        15,
                        () -> helper.assertTrue(counter.signal.getAsInt() > 0))
                .thenExecute("observe restored receiver", staysHigh::enable).thenIdle(5).thenSucceed();
    }

    @GameTest(template = "gate", timeoutTicks = 20)
    @MethodSource("implementations")
    public static void countClampsToValidatedMaximum(GameTestHelper helper, boolean ic) {
        CounterAccess counter = counter(helper, ic);
        fill(helper, counter);
        counter.maximum.accept(0);
        helper.assertEquals(1, counter.count.getAsInt(), "maximum zero is validated to one, not zero");
        helper.succeed();
    }

    @GameTest(template = "gate", timeoutTicks = 20)
    @MethodSource("implementations")
    public static void settingsAndValueRequestPersistence(GameTestHelper helper, boolean ic) {
        CounterAccess counter = counter(helper, ic);
        GateFixture.assertSaveRequested(helper, counter.owner, () -> counter.increment.accept(2));
        GateFixture.assertSaveRequested(helper, counter.owner, () -> counter.decrement.accept(3));
        GateFixture.assertSaveRequested(helper, counter.owner, () -> counter.maximum.accept(6));
        GateFixture.assertSaveRequested(helper, counter.owner, () -> counter.value.accept(2));
        helper.succeed();
    }

    @GameTest(template = "gate", timeoutTicks = 35)
    @MethodSource("implementations")
    public static void directValueEditsUpdateReceiverWithoutInputEdges(GameTestHelper helper, boolean ic) {
        CounterAccess counter = counter(helper, ic);
        long[] edited = { -1 };
        helper.startSequence()
                .thenWaitUntil(
                        "all initial work has settled",
                        10,
                        () -> helper.assertEquals(-1L, counter.scheduled.getAsLong()))
                .thenExecute("edit value directly to maximum", () -> {
                    helper.assertEquals(0, counter.signal.getAsInt());
                    edited[0] = helper.getWorld().getTotalWorldTime();
                    counter.value.accept(4);
                    helper.assertEquals(0, counter.signal.getAsInt(), "output must wait for its scheduled evaluation");
                })
                .thenWaitUntil(
                        "direct value edit powers attached wire",
                        6,
                        () -> helper.assertTrue(counter.signal.getAsInt() > 0))
                .thenExecute("power arrived after two ticks", () -> {
                    helper.assertEquals(2L, helper.getWorld().getTotalWorldTime() - edited[0]);
                    helper.assertEquals(-1L, counter.scheduled.getAsLong());
                    counter.value.accept(2);
                    edited[0] = helper.getWorld().getTotalWorldTime();
                    helper.assertTrue(counter.signal.getAsInt() > 0, "output must not change inline with setter");
                })
                .thenWaitUntil(
                        "nonterminal value clears attached wire",
                        6,
                        () -> helper.assertEquals(0, counter.signal.getAsInt()))
                .thenExecute(
                        "clear arrived after two ticks",
                        () -> helper.assertEquals(2L, helper.getWorld().getTotalWorldTime() - edited[0]))
                .thenSucceed();
    }

    private static void fill(GameTestHelper helper, CounterAccess counter) {
        for (int edge = 0; edge < 4; edge++) counter.pulse.run();
        helper.assertEquals(4, counter.count.getAsInt(), "precondition: counter must be at maximum");
    }

    private static CounterAccess counter(GameTestHelper helper, boolean ic) {
        CounterAccess access = new CounterAccess();
        if (ic) {
            SequentialGateICPart gate = GateFixture.ic(helper, ICGateDefinition.Counter());
            mrtjp.projectred.fabrication.Counter logic = (mrtjp.projectred.fabrication.Counter) gate.getLogic();
            logic.setCounterMax(gate, 4);
            LeverICPart lever = GateFixture.input(gate, 1);
            access.pulse = () -> {
                lever.readClientPacket(null);
                lever.readClientPacket(null);
            };
            access.count = logic::getCounterValue;
            access.maximum = n -> logic.setCounterMax(gate, n);
            access.increment = n -> logic.setCounterIncr(gate, n);
            access.decrement = n -> logic.setCounterDecr(gate, n);
            access.value = n -> logic.setCounterValue(gate, n);
            access.signal = GateFixture.output(gate)::getSignal;
            access.scheduled = gate::schedTime;
            access.owner = "workbench";
        } else {
            SequentialGatePart gate = (SequentialGatePart) GateFixture.world(helper, GateDefinition.Counter());
            Counter logic = (Counter) gate.getLogicSequential();
            access.pulse = () -> {
                helper.setRedstoneInput("port1", 15);
                helper.setRedstoneInput("port1", 0);
            };
            access.count = logic::getCounterValue;
            access.maximum = n -> logic.setCounterMax(gate, n);
            access.increment = n -> logic.setCounterIncr(gate, n);
            access.decrement = n -> logic.setCounterDecr(gate, n);
            access.value = n -> logic.setCounterValue(gate, n);
            access.signal = GateFixture.output(helper)::getSignal;
            access.scheduled = gate::schedTime;
            access.owner = "gate";
        }
        return access;
    }

    private static class CounterAccess {

        Runnable pulse;
        IntSupplier count;
        IntSupplier signal;
        LongSupplier scheduled;
        IntConsumer maximum;
        IntConsumer increment;
        IntConsumer decrement;
        IntConsumer value;
        String owner;
    }
}
